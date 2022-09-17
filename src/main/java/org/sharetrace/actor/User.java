package org.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.immutables.builder.Builder;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.Logger;
import org.sharetrace.logging.Logging;
import org.sharetrace.logging.event.ContactEvent;
import org.sharetrace.logging.event.ContactsRefreshEvent;
import org.sharetrace.logging.event.CurrentRefreshEvent;
import org.sharetrace.logging.event.LoggableEvent;
import org.sharetrace.logging.event.PropagateEvent;
import org.sharetrace.logging.event.ReceiveEvent;
import org.sharetrace.logging.event.SendCachedEvent;
import org.sharetrace.logging.event.SendCurrentEvent;
import org.sharetrace.logging.event.UpdateEvent;
import org.sharetrace.message.ContactMsg;
import org.sharetrace.message.RefreshMsg;
import org.sharetrace.message.RiskScoreMsg;
import org.sharetrace.message.TimeoutMsg;
import org.sharetrace.message.UserMsg;
import org.sharetrace.model.MsgParams;
import org.sharetrace.model.RiskScore;
import org.sharetrace.model.UserParams;
import org.sharetrace.util.IntervalCache;
import org.sharetrace.util.TypedSupplier;

/**
 * An actor that corresponds to a vertex in a {@link ContactNetwork}. Collectively, all {@link
 * User}s carry out the execution of {@link RiskPropagation}.
 *
 * @see RiskPropagation
 * @see RiskScoreMsg
 * @see ContactMsg
 * @see TimeoutMsg
 * @see RefreshMsg
 * @see UserParams
 * @see MsgParams
 * @see IntervalCache
 */
public final class User extends AbstractBehavior<UserMsg> {

  private final TimerScheduler<UserMsg> timers;
  private final Logger logger;
  private final Map<ActorRef<UserMsg>, Instant> contacts;
  private final UserParams userParams;
  private final MsgParams msgParams;
  private final Clock clock;
  private final IntervalCache<RiskScoreMsg> cache;
  private RiskScoreMsg prev;
  private RiskScoreMsg curr;
  private RiskScoreMsg transmitted;
  private double sendThresh;

  private User(
      ActorContext<UserMsg> ctx,
      TimerScheduler<UserMsg> timers,
      Set<Class<? extends Loggable>> loggable,
      UserParams userParams,
      MsgParams msgParams,
      Clock clock,
      IntervalCache<RiskScoreMsg> cache) {
    super(ctx);
    this.timers = timers;
    this.logger = Logging.logger(loggable, getContext()::getLog);
    this.contacts = new Object2ObjectOpenHashMap<>();
    this.userParams = userParams;
    this.msgParams = msgParams;
    this.clock = clock;
    this.cache = cache;
    this.curr = defaultMsg();
    updateWith(curr);
    startRefreshTimer();
  }

  @Builder.Factory
  static Behavior<UserMsg> user(
      Map<String, String> mdc,
      Set<Class<? extends Loggable>> loggable,
      UserParams userParams,
      MsgParams msgParams,
      Clock clock,
      IntervalCache<RiskScoreMsg> cache) {
    return Behaviors.setup(
        ctx -> {
          ctx.setLoggerName(Logging.eventsLoggerName());
          Behavior<UserMsg> user =
              Behaviors.withTimers(
                  timers -> new User(ctx, timers, loggable, userParams, msgParams, clock, cache));
          return Behaviors.withMdc(UserMsg.class, message -> mdc, user);
        });
  }

  private static Predicate<Entry<ActorRef<UserMsg>, ?>> isNotFrom(RiskScoreMsg received) {
    return Predicate.not(contact -> contact.getKey().equals(received.replyTo()));
  }

  private static SendCurrentEvent sendCurrentEvent(ActorRef<UserMsg> contact, RiskScoreMsg curr) {
    return SendCurrentEvent.builder()
        .from(name(curr.replyTo()))
        .to(name(contact))
        .score(curr.score())
        .id(curr.id())
        .build();
  }

  private static SendCachedEvent sendCachedEvent(ActorRef<UserMsg> contact, RiskScoreMsg cached) {
    return SendCachedEvent.builder()
        .from(name(cached.replyTo()))
        .to(name(contact))
        .score(cached.score())
        .id(cached.id())
        .build();
  }

  private static PropagateEvent propagateEvent(ActorRef<UserMsg> contact, RiskScoreMsg propagated) {
    return PropagateEvent.builder()
        .from(name(propagated.replyTo()))
        .to(name(contact))
        .score(propagated.score())
        .id(propagated.id())
        .build();
  }

  private static String name(ActorRef<UserMsg> user) {
    return user.path().name();
  }

  @Override
  public Receive<UserMsg> createReceive() {
    return newReceiveBuilder()
        .onMessage(ContactMsg.class, this::onContactMsg)
        .onMessage(RiskScoreMsg.class, this::onRiskScoreMsg)
        .onMessage(TimeoutMsg.class, x -> Behaviors.stopped())
        .onMessage(RefreshMsg.class, this::onRefreshMsg)
        .build();
  }

  private RiskScoreMsg defaultMsg() {
    return RiskScoreMsg.builder()
        .score(RiskScore.ofMinValue(clock.instant()))
        .replyTo(getContext().getSelf())
        .build();
  }

  private void updateWith(RiskScoreMsg msg) {
    prev = curr;
    curr = msg;
    transmitted = transmitted(curr);
    sendThresh = curr.score().value() * msgParams.sendCoeff();
  }

  private RiskScoreMsg transmitted(RiskScoreMsg msg) {
    return RiskScoreMsg.builder()
        .replyTo(getContext().getSelf())
        .score(transmitted(msg.score()))
        .id(msg.id())
        .build();
  }

  private RiskScore transmitted(RiskScore score) {
    return score.withValue((float) (score.value() * msgParams.transRate()));
  }

  private void startRefreshTimer() {
    timers.startTimerWithFixedDelay(RefreshMsg.INSTANCE, userParams.refreshPeriod());
  }

  private Behavior<UserMsg> onContactMsg(ContactMsg msg) {
    addContactIfAlive(msg);
    sendToContact(msg);
    return this;
  }

  private void sendToContact(ContactMsg msg) {
    if (isScoreAlive(curr) && isScoreRecent(curr, msg.contactTime())) {
      sendCurrentTo(msg.contact());
    } else {
      sendCachedTo(msg);
    }
  }

  private Behavior<UserMsg> onRiskScoreMsg(RiskScoreMsg received) {
    logReceive(received);
    propagate(updateAndCache(received));
    resetTimeout();
    return this;
  }

  private Behavior<UserMsg> onRefreshMsg(RefreshMsg msg) {
    refreshContacts();
    refreshCurrent();
    return this;
  }

  private void sendCurrentTo(ActorRef<UserMsg> contact) {
    contact.tell(transmitted);
    logSendCurrent(contact);
  }

  private void resetTimeout() {
    timers.startSingleTimer(TimeoutMsg.INSTANCE, userParams.idleTimeout());
  }

  private void logSendCurrent(ActorRef<UserMsg> contact) {
    logEvent(SendCurrentEvent.class, () -> sendCurrentEvent(contact, transmitted));
  }

  private void refreshContacts() {
    int numContacts = contacts.size();
    contacts.values().removeIf(Predicate.not(this::isContactAlive));
    int numRemaining = contacts.size();
    logContactsRefresh(numRemaining, numContacts - numRemaining);
  }

  private void refreshCurrent() {
    if (!isScoreAlive(curr)) {
      updateWith(maxCachedOrDefault());
      logCurrentRefresh();
    }
  }

  private RiskScoreMsg maxCachedOrDefault() {
    return cache.max(clock.instant()).orElseGet(this::defaultMsg);
  }

  private void addContactIfAlive(ContactMsg msg) {
    if (isContactAlive(msg.contactTime())) {
      contacts.put(msg.contact(), msg.contactTime());
      logContact(msg.contact());
    }
  }

  private void sendCachedTo(ContactMsg msg) {
    Instant contactTime = buffered(msg.contactTime());
    cache.max(contactTime).ifPresent(cached -> sendCachedTo(msg.contact(), cached));
  }

  private Predicate<Entry<?, Instant>> isContactRecent(RiskScoreMsg relativeTo) {
    return contact -> isScoreRecent(relativeTo, contact.getValue());
  }

  private void sendCachedTo(ActorRef<UserMsg> contact, RiskScoreMsg cached) {
    if (isScoreAlive(cached)) {
      contact.tell(cached);
      logSendCached(contact, cached);
    }
  }

  private void propagate(RiskScoreMsg msg) {
    if (isScoreAlive(msg) && isHighEnough(msg)) {
      contacts.entrySet().stream()
          .filter(isNotFrom(msg))
          .filter(isContactRecent(msg))
          .map(Entry::getKey)
          .forEach(contact -> propagate(contact, msg));
    }
  }

  private boolean isHighEnough(RiskScoreMsg msg) {
    return msg.score().value() >= sendThresh;
  }

  private void logSendCached(ActorRef<UserMsg> contact, RiskScoreMsg cached) {
    logEvent(SendCachedEvent.class, () -> sendCachedEvent(contact, cached));
  }

  private boolean isScoreRecent(RiskScoreMsg msg, Instant notAfter) {
    return !msg.score().time().isAfter(buffered(notAfter));
  }

  private Instant buffered(Instant time) {
    return time.plus(msgParams.timeBuffer());
  }

  private boolean isContactAlive(Temporal contactTime) {
    return isAlive(contactTime, msgParams.contactTtl());
  }

  private boolean isScoreAlive(RiskScoreMsg msg) {
    return isAlive(msg.score().time(), msgParams.scoreTtl());
  }

  private void propagate(ActorRef<UserMsg> contact, RiskScoreMsg msg) {
    contact.tell(msg);
    logPropagate(contact, msg);
  }

  private void logPropagate(ActorRef<UserMsg> contact, RiskScoreMsg propagated) {
    logEvent(PropagateEvent.class, () -> propagateEvent(contact, propagated));
  }

  private void logContact(ActorRef<UserMsg> contact) {
    logEvent(ContactEvent.class, () -> contactEvent(contact));
  }

  private void logContactsRefresh(int numRemaining, int numExpired) {
    logEvent(ContactsRefreshEvent.class, () -> contactsRefreshEvent(numRemaining, numExpired));
  }

  private ContactsRefreshEvent contactsRefreshEvent(int numRemaining, int numExpired) {
    return ContactsRefreshEvent.builder()
        .user(name())
        .numRemaining(numRemaining)
        .numExpired(numExpired)
        .build();
  }

  private void logCurrentRefresh() {
    logEvent(CurrentRefreshEvent.class, this::currentRefreshEvent);
  }

  private CurrentRefreshEvent currentRefreshEvent() {
    return CurrentRefreshEvent.builder()
        .user(name())
        .oldScore(prev.score())
        .newScore(curr.score())
        .oldId(prev.id())
        .newId(curr.id())
        .build();
  }

  private RiskScoreMsg updateAndCache(RiskScoreMsg msg) {
    RiskScoreMsg propagate;
    if (msg.score().value() > curr.score().value()) {
      updateWith(msg);
      logUpdate();
      propagate = transmitted;
    } else {
      propagate = transmitted(msg);
    }
    cache.put(msg.score().time(), propagate);
    return propagate;
  }

  private void logUpdate() {
    logEvent(UpdateEvent.class, this::updateEvent);
  }

  private <T extends Loggable> void logEvent(Class<T> type, Supplier<T> supplier) {
    logger.log(LoggableEvent.KEY, TypedSupplier.of(type, supplier));
  }

  private UpdateEvent updateEvent() {
    return UpdateEvent.builder()
        .from(name(curr.replyTo()))
        .to(name())
        .oldScore(prev.score())
        .newScore(curr.score())
        .oldId(prev.id())
        .newId(curr.id())
        .build();
  }

  private String name() {
    return name(getContext().getSelf());
  }

  private ContactEvent contactEvent(ActorRef<UserMsg> contact) {
    return ContactEvent.builder().user(name()).addUsers(name(), name(contact)).build();
  }

  private void logReceive(RiskScoreMsg received) {
    logEvent(ReceiveEvent.class, () -> receiveEvent(received));
  }

  private ReceiveEvent receiveEvent(RiskScoreMsg received) {
    return ReceiveEvent.builder()
        .from(name(received.replyTo()))
        .to(name())
        .score(received.score())
        .id(received.id())
        .build();
  }

  private boolean isAlive(Temporal temporal, Duration ttl) {
    return Duration.between(temporal, clock.instant()).compareTo(ttl) < 0;
  }
}

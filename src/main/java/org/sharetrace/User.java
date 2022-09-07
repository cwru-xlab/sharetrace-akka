package org.sharetrace;

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
import org.sharetrace.logging.events.ContactEvent;
import org.sharetrace.logging.events.ContactsRefreshEvent;
import org.sharetrace.logging.events.CurrentRefreshEvent;
import org.sharetrace.logging.events.LoggableEvent;
import org.sharetrace.logging.events.PropagateEvent;
import org.sharetrace.logging.events.ReceiveEvent;
import org.sharetrace.logging.events.SendCachedEvent;
import org.sharetrace.logging.events.SendCurrentEvent;
import org.sharetrace.logging.events.UpdateEvent;
import org.sharetrace.message.ContactMsg;
import org.sharetrace.message.MsgParams;
import org.sharetrace.message.RefreshMsg;
import org.sharetrace.message.RiskScore;
import org.sharetrace.message.RiskScoreMsg;
import org.sharetrace.message.TimeoutMsg;
import org.sharetrace.message.UserMsg;
import org.sharetrace.message.UserParams;
import org.sharetrace.util.IntervalCache;
import org.sharetrace.util.TypedSupplier;

/**
 * An actor that corresponds to a {@link ContactNetwork} node. Collectively, all {@link User}s carry
 * out the execution of {@link RiskPropagation}.
 *
 * @see UserParams
 */
public class User extends AbstractBehavior<UserMsg> {

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
  private float sendThresh;

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
    update(curr);
    startRefreshTimer();
  }

  private RiskScoreMsg defaultMsg() {
    return RiskScoreMsg.builder()
        .score(RiskScore.ofMinValue(clock.instant()))
        .replyTo(getContext().getSelf())
        .build();
  }

  private void update(RiskScoreMsg msg) {
    prev = curr;
    curr = msg;
    transmitted = transmitted(curr);
    sendThresh = curr.score().value() * msgParams.sendCoefficient();
  }

  private RiskScoreMsg transmitted(RiskScoreMsg msg) {
    return RiskScoreMsg.builder()
        .replyTo(getContext().getSelf())
        .score(transmittedScore(msg))
        .id(msg.id())
        .build();
  }

  private RiskScore transmittedScore(RiskScoreMsg msg) {
    return RiskScore.builder()
        .value(msg.score().value() * msgParams.transmissionRate())
        .time(msg.score().time())
        .build();
  }

  private void startRefreshTimer() {
    timers.startTimerWithFixedDelay(RefreshMsg.INSTANCE, userParams.refreshPeriod());
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

  private static Predicate<Entry<?, ?>> isNotFrom(RiskScoreMsg msg) {
    return Predicate.not(contact -> contact.getKey().equals(msg.replyTo()));
  }

  private static SendCurrentEvent sendCurrentEvent(ActorRef<UserMsg> contact, RiskScoreMsg msg) {
    return SendCurrentEvent.builder()
        .from(name(msg.replyTo()))
        .to(name(contact))
        .score(msg.score())
        .id(msg.id())
        .build();
  }

  private static SendCachedEvent sendCachedEvent(ActorRef<UserMsg> contact, RiskScoreMsg msg) {
    return SendCachedEvent.builder()
        .from(name(msg.replyTo()))
        .to(name(contact))
        .score(msg.score())
        .id(msg.id())
        .build();
  }

  private static PropagateEvent propagateEvent(ActorRef<UserMsg> contact, RiskScoreMsg msg) {
    return PropagateEvent.builder()
        .from(name(msg.replyTo()))
        .to(name(contact))
        .score(msg.score())
        .id(msg.id())
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

  private Behavior<UserMsg> onContactMsg(ContactMsg msg) {
    addContactIfAlive(msg);
    sendToContact(msg);
    return this;
  }

  private void sendToContact(ContactMsg msg) {
    if (isScoreAlive(curr) && isContactRecent(msg, curr)) {
      sendCurrent(msg);
    } else {
      sendCached(msg);
    }
  }

  private Behavior<UserMsg> onRiskScoreMsg(RiskScoreMsg msg) {
    logReceive(msg);
    propagate(updateAndCache(msg));
    resetTimeout();
    return this;
  }

  private Behavior<UserMsg> onRefreshMsg(RefreshMsg msg) {
    refreshContacts();
    refreshCurrent();
    return this;
  }

  private void sendCurrent(ContactMsg msg) {
    ActorRef<UserMsg> contact = msg.replyTo();
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
    int nContacts = contacts.size();
    contacts.values().removeIf(Predicate.not(this::isContactAlive));
    int numRemaining = contacts.size();
    int numExpired = nContacts - numRemaining;
    logContactsRefresh(numRemaining, numExpired);
  }

  private void refreshCurrent() {
    if (!isScoreAlive(curr)) {
      update(maxCachedOrDefault());
      logCurrentRefresh();
    }
  }

  private RiskScoreMsg maxCachedOrDefault() {
    return cache.max(clock.instant()).orElseGet(this::defaultMsg);
  }

  private void addContactIfAlive(ContactMsg msg) {
    if (isContactAlive(msg.time())) {
      contacts.put(msg.replyTo(), msg.time());
      logContact(msg);
    }
  }

  private void sendCached(ContactMsg msg) {
    Instant buffered = buffered(msg.time());
    cache.max(buffered).ifPresent(cached -> sendCached(msg.replyTo(), cached));
  }

  private Predicate<Entry<?, Instant>> isContactRecent(RiskScoreMsg msg) {
    return contact -> isRecent(contact.getValue(), msg);
  }

  private void sendCached(ActorRef<UserMsg> contact, RiskScoreMsg cached) {
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

  private void logSendCached(ActorRef<UserMsg> contact, RiskScoreMsg msg) {
    logEvent(SendCachedEvent.class, () -> sendCachedEvent(contact, msg));
  }

  private boolean isContactRecent(ContactMsg contact, RiskScoreMsg msg) {
    return isRecent(contact.time(), msg);
  }

  private boolean isRecent(Instant time, RiskScoreMsg msg) {
    // Message time is no newer (inclusive) than the buffered time.
    return !msg.score().time().isAfter(buffered(time));
  }

  private Instant buffered(Instant time) {
    return time.plus(msgParams.timeBuffer());
  }

  private boolean isContactAlive(Instant time) {
    return isAlive(time, msgParams.contactTtl());
  }

  private boolean isScoreAlive(RiskScoreMsg msg) {
    return isAlive(msg.score().time(), msgParams.scoreTtl());
  }

  private void propagate(ActorRef<UserMsg> contact, RiskScoreMsg msg) {
    contact.tell(msg);
    logPropagate(contact, msg);
  }

  private void logPropagate(ActorRef<UserMsg> contact, RiskScoreMsg msg) {
    logEvent(PropagateEvent.class, () -> propagateEvent(contact, msg));
  }

  private void logContact(ContactMsg msg) {
    logEvent(ContactEvent.class, () -> contactEvent(msg));
  }

  private void logContactsRefresh(int numRemaining, int numExpired) {
    logEvent(ContactsRefreshEvent.class, () -> contactsRefreshEvent(numRemaining, numExpired));
  }

  private ContactsRefreshEvent contactsRefreshEvent(int numRemaining, int numExpired) {
    return ContactsRefreshEvent.builder()
        .of(name())
        .numRemaining(numRemaining)
        .numExpired(numExpired)
        .build();
  }

  private void logCurrentRefresh() {
    logEvent(CurrentRefreshEvent.class, this::currentRefreshEvent);
  }

  private CurrentRefreshEvent currentRefreshEvent() {
    return CurrentRefreshEvent.builder()
        .of(name())
        .oldScore(prev.score())
        .newScore(curr.score())
        .oldId(prev.id())
        .newId(curr.id())
        .build();
  }

  private RiskScoreMsg updateAndCache(RiskScoreMsg msg) {
    RiskScoreMsg propagate;
    if (isHigherThanCurrent(msg)) {
      update(msg);
      logUpdate();
      propagate = transmitted;
    } else {
      propagate = transmitted(msg);
    }
    return cached(propagate);
  }

  private boolean isHigherThanCurrent(RiskScoreMsg msg) {
    // Do NOT use scoreTolerance; otherwise, it causes issues with logging and analysis.
    return msg.score().value() > curr.score().value();
  }

  private RiskScoreMsg cached(RiskScoreMsg msg) {
    cache.put(msg.score().time(), msg);
    return msg;
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

  private ContactEvent contactEvent(ContactMsg msg) {
    return ContactEvent.builder().of(name()).addUsers(name(), name(msg.replyTo())).build();
  }

  private void logReceive(RiskScoreMsg msg) {
    logEvent(ReceiveEvent.class, () -> receiveEvent(msg));
  }

  private ReceiveEvent receiveEvent(RiskScoreMsg msg) {
    return ReceiveEvent.builder()
        .from(name(msg.replyTo()))
        .to(name())
        .score(msg.score())
        .id(msg.id())
        .build();
  }

  private boolean isAlive(Instant time, Duration timeToLive) {
    return Duration.between(time, clock.instant()).compareTo(timeToLive) < 0;
  }
}

package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.logging.Loggable;
import io.sharetrace.logging.Logger;
import io.sharetrace.logging.Logging;
import io.sharetrace.logging.event.ContactEvent;
import io.sharetrace.logging.event.ContactsRefreshEvent;
import io.sharetrace.logging.event.CurrentRefreshEvent;
import io.sharetrace.logging.event.LoggableEvent;
import io.sharetrace.logging.event.PropagateEvent;
import io.sharetrace.logging.event.ReceiveEvent;
import io.sharetrace.logging.event.SendCachedEvent;
import io.sharetrace.logging.event.SendCurrentEvent;
import io.sharetrace.logging.event.TimeoutEvent;
import io.sharetrace.logging.event.UpdateEvent;
import io.sharetrace.message.ContactMsg;
import io.sharetrace.message.RefreshMsg;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.message.TimeoutMsg;
import io.sharetrace.message.UserMsg;
import io.sharetrace.model.MsgParams;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.UserParams;
import io.sharetrace.util.IntervalCache;
import io.sharetrace.util.TypedSupplier;
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
  private final Map<ActorRef<UserMsg>, ContactInfo> contacts;
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
          ctx.setLoggerName(Logging.EVENTS_LOGGER_NAME);
          Behavior<UserMsg> user =
              Behaviors.withTimers(
                  timers -> new User(ctx, timers, loggable, userParams, msgParams, clock, cache));
          return Behaviors.withMdc(UserMsg.class, msg -> mdc, user);
        });
  }

  private static boolean isFrom(ActorRef<UserMsg> contact, RiskScoreMsg received) {
    return contact.equals(received.replyTo());
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
        .onMessage(TimeoutMsg.class, this::onTimeoutMsg)
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
    return score.withValue(score.value() * msgParams.transRate());
  }

  private void startRefreshTimer() {
    timers.startTimerWithFixedDelay(RefreshMsg.INSTANCE, userParams.refreshPeriod());
  }

  private Behavior<UserMsg> onContactMsg(ContactMsg msg) {
    if (addContactIfAlive(msg)) {
      sendToContact(msg);
    }
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

  private Behavior<UserMsg> onTimeoutMsg(TimeoutMsg msg) {
    logTimeout();
    return Behaviors.stopped();
  }

  private void sendCurrentTo(ActorRef<UserMsg> contact) {
    tell(contact, transmitted);
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

  private boolean addContactIfAlive(ContactMsg msg) {
    boolean added = isContactAlive(msg.contactTime());
    if (added) {
      contacts.put(msg.contact(), new ContactInfo(msg.contactTime()));
      logContact(msg.contact());
    }
    return added;
  }

  private void sendCachedTo(ContactMsg msg) {
    Instant contactTime = buffered(msg.contactTime());
    cache.max(contactTime).ifPresent(cached -> sendCachedTo(msg.contact(), cached));
  }

  private boolean isContactRecent(ContactInfo contactInfo, RiskScoreMsg relativeTo) {
    return isScoreRecent(relativeTo, contactInfo.contactTime);
  }

  private void sendCachedTo(ActorRef<UserMsg> contact, RiskScoreMsg cached) {
    if (isScoreAlive(cached)) {
      tell(contact, cached);
      logSendCached(contact, cached);
    }
  }

  private void tell(ActorRef<UserMsg> contact, RiskScoreMsg msg) {
    contact.tell(msg);
    contacts.get(contact).lastSent = msg.score();
  }

  private void propagate(RiskScoreMsg msg) {
    if (isScoreAlive(msg) && isHighEnough(msg)) {
      contacts.entrySet().stream()
          .filter(entry -> shouldPropagateTo(entry, msg))
          .map(Entry::getKey)
          .forEach(contact -> propagate(contact, msg));
    }
  }

  private boolean shouldPropagateTo(Entry<ActorRef<UserMsg>, ContactInfo> entry, RiskScoreMsg msg) {
    ActorRef<UserMsg> contact = entry.getKey();
    ContactInfo info = entry.getValue();
    return !isFrom(contact, msg) && isContactRecent(info, msg) && isHighEnough(msg, info);
  }

  private boolean isHighEnough(RiskScoreMsg msg) {
    return msg.score().value() > sendThresh;
  }

  private boolean isHighEnough(RiskScoreMsg msg, ContactInfo contactInfo) {
    return msg.score().value() > contactInfo.lastSent.value() + msgParams.tolerance();
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

  private boolean isContactAlive(ContactInfo contactInfo) {
    return isContactAlive(contactInfo.contactTime);
  }

  private boolean isContactAlive(Temporal contactTime) {
    return isAlive(contactTime, msgParams.contactTtl());
  }

  private boolean isScoreAlive(RiskScoreMsg msg) {
    return isAlive(msg.score().time(), msgParams.scoreTtl());
  }

  private void propagate(ActorRef<UserMsg> contact, RiskScoreMsg msg) {
    tell(contact, msg);
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

  private void logTimeout() {
    logEvent(TimeoutEvent.class, this::timeoutEvent);
  }

  private TimeoutEvent timeoutEvent() {
    return TimeoutEvent.builder().user(name()).build();
  }

  private boolean isAlive(Temporal temporal, Duration ttl) {
    return Duration.between(temporal, clock.instant()).compareTo(ttl) < 0;
  }

  private static final class ContactInfo {

    private static final RiskScore DEFAULT_LAST_SENT = RiskScore.ofMinValue(Instant.EPOCH);
    private final Instant contactTime;
    private RiskScore lastSent = DEFAULT_LAST_SENT;

    public ContactInfo(Instant contactTime) {
      this.contactTime = contactTime;
    }
  }
}

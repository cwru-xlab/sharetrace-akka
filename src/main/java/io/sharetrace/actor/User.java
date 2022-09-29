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
import io.sharetrace.message.ThresholdMsg;
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
import java.util.Set;
import java.util.function.BiConsumer;
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
  private final EventLogger logger;
  private final UserParams userParams;
  private final Clock clock;
  private final IntervalCache<RiskScoreMsg> cache;
  private final Map<ActorRef<UserMsg>, ContactActor> contacts;
  private final MsgUtil msgUtil;
  private final RiskScoreMsg defaultMsg;
  private RiskScoreMsg current;
  private RiskScoreMsg transmitted;

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
    this.logger = new EventLogger(loggable, getContext());
    this.userParams = userParams;
    this.clock = clock;
    this.cache = cache;
    this.contacts = new Object2ObjectOpenHashMap<>();
    this.msgUtil = new MsgUtil(getContext(), clock, msgParams);
    this.defaultMsg = msgUtil.defaultMsg();
    updateWith(defaultMsg);
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

  @Override
  public Receive<UserMsg> createReceive() {
    return newReceiveBuilder()
        .onMessage(ContactMsg.class, this::onContactMsg)
        .onMessage(RiskScoreMsg.class, this::onRiskScoreMsg)
        .onMessage(TimeoutMsg.class, this::onTimeoutMsg)
        .onMessage(RefreshMsg.class, this::onRefreshMsg)
        .onMessage(ThresholdMsg.class, this::onThresholdMsg)
        .build();
  }

  private RiskScoreMsg updateWith(RiskScoreMsg msg) {
    RiskScoreMsg previous = current;
    current = msg;
    transmitted = msgUtil.transmitted(msg);
    return previous;
  }

  private void startRefreshTimer() {
    timers.startTimerWithFixedDelay(RefreshMsg.INSTANCE, userParams.refreshPeriod());
  }

  private Behavior<UserMsg> onContactMsg(ContactMsg msg) {
    if (msgUtil.isAlive(msg)) {
      ContactActor contact = new ContactActor(msg, timers, msgUtil, cache, defaultMsg);
      contacts.put(contact.ref, contact);
      logger.logContact(contact.ref);
      sendToContact(contact);
    }
    return this;
  }

  private void sendToContact(ContactActor contact) {
    if (contact.shouldReceive(current)) {
      sendCurrent(contact);
    } else {
      sendCached(contact);
    }
  }

  private void sendCurrent(ContactActor contact) {
    contact.tell(transmitted, logger::logSendCurrent);
  }

  private void sendCached(ContactActor contact) {
    cache
        .max(contact.bufferedContactTime())
        .filter(msgUtil::isAlive)
        .ifPresent(cached -> contact.tell(cached, logger::logSendCached));
  }

  private Behavior<UserMsg> onRiskScoreMsg(RiskScoreMsg received) {
    logger.logReceive(received);
    propagate(updateAndCache(received));
    resetTimeout();
    return this;
  }

  @SuppressWarnings("unused")
  private Behavior<UserMsg> onRefreshMsg(RefreshMsg msg) {
    refreshContacts();
    refreshCurrent();
    return this;
  }

  private Behavior<UserMsg> onThresholdMsg(ThresholdMsg msg) {
    contacts.get(msg.contact()).updateThreshold();
    return this;
  }

  @SuppressWarnings("unused")
  private Behavior<UserMsg> onTimeoutMsg(TimeoutMsg msg) {
    logger.logTimeout();
    return Behaviors.stopped();
  }

  private void resetTimeout() {
    timers.startSingleTimer(TimeoutMsg.INSTANCE, userParams.idleTimeout());
  }

  private void refreshContacts() {
    int numContacts = contacts.size();
    contacts.values().removeIf(Predicate.not(ContactActor::isAlive));
    int numRemaining = contacts.size();
    logger.logContactsRefresh(numRemaining, numContacts - numRemaining);
  }

  private void refreshCurrent() {
    if (!msgUtil.isAlive(current)) {
      RiskScoreMsg previous = updateWith(cache.max(clock.instant()).orElse(defaultMsg));
      logger.logCurrentRefresh(previous, current);
    }
  }

  private void propagate(RiskScoreMsg msg) {
    if (msgUtil.isAlive(msg)) {
      contacts.values().stream()
          .filter(contact -> contact.shouldReceive(msg))
          .forEach(contact -> contact.tell(msg, logger::logPropagate));
    }
  }

  private RiskScoreMsg updateAndCache(RiskScoreMsg msg) {
    RiskScoreMsg propagate;
    if (msgUtil.isGreaterThan(msg, current)) {
      RiskScoreMsg previous = updateWith(msg);
      logger.logUpdate(previous, current);
      propagate = transmitted;
    } else {
      propagate = msgUtil.transmitted(msg);
    }
    cache.put(msg.score().time(), propagate);
    return propagate;
  }

  private static final class ContactActor {

    private final ActorRef<UserMsg> ref;
    private final Instant contactTime;
    private final IntervalCache<RiskScoreMsg> cache;
    private final MsgUtil msgUtil;
    private final TimerScheduler<UserMsg> timers;
    private final RiskScoreMsg defaultMsg;
    private RiskScoreMsg thresholdMsg;
    private float sendThreshold;

    public ContactActor(
        ContactMsg msg,
        TimerScheduler<UserMsg> timers,
        MsgUtil msgUtil,
        IntervalCache<RiskScoreMsg> cache,
        RiskScoreMsg defaultMsg) {
      this.ref = msg.contact();
      this.contactTime = msg.contactTime();
      this.cache = cache;
      this.msgUtil = msgUtil;
      this.timers = timers;
      this.defaultMsg = defaultMsg;
      this.thresholdMsg = defaultMsg;
      this.sendThreshold = RiskScore.MIN_VALUE;
    }

    public boolean shouldReceive(RiskScoreMsg msg) {
      return exceedsThreshold(msg) && isRelevant(msg) && msgUtil.isAlive(msg) && isNotSender(msg);
    }

    public boolean exceedsThreshold(RiskScoreMsg msg) {
      return msgUtil.isGreaterThan(msg, sendThreshold);
    }

    public boolean isRelevant(RiskScoreMsg msg) {
      return msgUtil.isNotAfter(msg, bufferedContactTime());
    }

    public boolean isNotSender(RiskScoreMsg msg) {
      return !ref.equals(msg.replyTo());
    }

    public Instant bufferedContactTime() {
      return msgUtil.buffered(contactTime);
    }

    public void tell(RiskScoreMsg msg, BiConsumer<ActorRef<?>, RiskScoreMsg> logEvent) {
      ref.tell(msg);
      logEvent.accept(ref, msg);
      updateThreshold(msg);
    }

    public void updateThreshold() {
      thresholdMsg = cache.max(bufferedContactTime()).filter(msgUtil::isAlive).orElse(defaultMsg);
      sendThreshold = msgUtil.computeThreshold(thresholdMsg);
    }

    private void updateThreshold(RiskScoreMsg msg) {
      float threshold = msgUtil.computeThreshold(msg);
      if (threshold > sendThreshold) {
        sendThreshold = threshold;
        thresholdMsg = msg;
        timers.startSingleTimer(ThresholdMsg.of(ref), msgUtil.computeTtl(thresholdMsg));
      }
    }

    public boolean isAlive() {
      return msgUtil.isAlive(contactTime);
    }
  }

  private static final class MsgUtil {

    private final ActorContext<UserMsg> ctx;
    private final Clock clock;
    private final MsgParams params;

    public MsgUtil(ActorContext<UserMsg> ctx, Clock clock, MsgParams params) {
      this.ctx = ctx;
      this.clock = clock;
      this.params = params;
    }

    public boolean isNotAfter(RiskScoreMsg msg, Instant time) {
      return !msg.score().time().isAfter(time);
    }

    public float computeThreshold(RiskScoreMsg msg) {
      return msg.score().value() * params.sendCoeff();
    }

    public Instant buffered(Instant time) {
      return time.plus(params.timeBuffer());
    }

    public RiskScoreMsg transmitted(RiskScoreMsg msg) {
      RiskScore original = msg.score();
      RiskScore modified = original.withValue(original.value() * params.transRate());
      return RiskScoreMsg.builder().score(modified).replyTo(ctx.getSelf()).id(msg.id()).build();
    }

    public RiskScoreMsg defaultMsg() {
      return RiskScoreMsg.builder().score(RiskScore.MIN).replyTo(ctx.getSelf()).build();
    }

    public boolean isGreaterThan(RiskScoreMsg msg1, RiskScoreMsg msg2) {
      return isGreaterThan(msg1, msg2.score().value());
    }

    public boolean isGreaterThan(RiskScoreMsg msg, float value) {
      return msg.score().value() > value;
    }

    public Duration computeTtl(RiskScoreMsg msg) {
      Duration sinceComputed = elapsedSince(msg.score().time());
      return params.scoreTtl().minus(sinceComputed);
    }

    public boolean isAlive(RiskScoreMsg msg) {
      return isAlive(msg.score().time(), params.scoreTtl());
    }

    public boolean isAlive(ContactMsg msg) {
      return isAlive(msg.contactTime());
    }

    public boolean isAlive(Instant contactTime) {
      return isAlive(contactTime, params.contactTtl());
    }

    private boolean isAlive(Temporal temporal, Duration ttl) {
      return elapsedSince(temporal).compareTo(ttl) < 0;
    }

    private Duration elapsedSince(Temporal temporal) {
      return Duration.between(temporal, clock.instant());
    }
  }

  private static final class EventLogger {

    private final Logger logger;
    private final String userName;

    private EventLogger(Set<Class<? extends Loggable>> loggable, ActorContext<?> context) {
      this.logger = Logging.logger(loggable, context::getLog);
      this.userName = name(context.getSelf());
    }

    private static String name(ActorRef<?> user) {
      return user.path().name();
    }

    public void logContact(ActorRef<?> contact) {
      log(ContactEvent.class, () -> contactEvent(contact));
    }

    public void logSendCached(ActorRef<?> contact, RiskScoreMsg cached) {
      log(SendCachedEvent.class, () -> sendCachedEvent(contact, cached));
    }

    public void logSendCurrent(ActorRef<?> contact, RiskScoreMsg current) {
      log(SendCurrentEvent.class, () -> sendCurrentEvent(contact, current));
    }

    public void logReceive(RiskScoreMsg received) {
      log(ReceiveEvent.class, () -> receiveEvent(received));
    }

    public void logUpdate(RiskScoreMsg previous, RiskScoreMsg current) {
      log(UpdateEvent.class, () -> updateEvent(previous, current));
    }

    public void logPropagate(ActorRef<?> contact, RiskScoreMsg propagated) {
      log(PropagateEvent.class, () -> propagateEvent(contact, propagated));
    }

    public void logContactsRefresh(int numRemaining, int numExpired) {
      log(ContactsRefreshEvent.class, () -> contactsRefreshEvent(numRemaining, numExpired));
    }

    public void logCurrentRefresh(RiskScoreMsg previous, RiskScoreMsg current) {
      log(CurrentRefreshEvent.class, () -> currentRefreshEvent(previous, current));
    }

    public void logTimeout() {
      log(TimeoutEvent.class, this::timeoutEvent);
    }

    private <T extends Loggable> void log(Class<T> type, Supplier<T> supplier) {
      logger.log(LoggableEvent.KEY, TypedSupplier.of(type, supplier));
    }

    private ContactEvent contactEvent(ActorRef<?> contact) {
      return ContactEvent.builder().user(userName).addUsers(userName, name(contact)).build();
    }

    private SendCachedEvent sendCachedEvent(ActorRef<?> contact, RiskScoreMsg cached) {
      return SendCachedEvent.builder()
          .from(name(cached.replyTo()))
          .to(name(contact))
          .score(cached.score())
          .id(cached.id())
          .build();
    }

    private SendCurrentEvent sendCurrentEvent(ActorRef<?> contact, RiskScoreMsg current) {
      return SendCurrentEvent.builder()
          .from(name(current.replyTo()))
          .to(name(contact))
          .score(current.score())
          .id(current.id())
          .build();
    }

    private ReceiveEvent receiveEvent(RiskScoreMsg received) {
      return ReceiveEvent.builder()
          .from(name(received.replyTo()))
          .to(userName)
          .score(received.score())
          .id(received.id())
          .build();
    }

    private UpdateEvent updateEvent(RiskScoreMsg previous, RiskScoreMsg current) {
      return UpdateEvent.builder()
          .from(name(current.replyTo()))
          .to(userName)
          .oldScore(previous.score())
          .newScore(current.score())
          .oldId(previous.id())
          .newId(current.id())
          .build();
    }

    private PropagateEvent propagateEvent(ActorRef<?> contact, RiskScoreMsg propagated) {
      return PropagateEvent.builder()
          .from(name(propagated.replyTo()))
          .to(name(contact))
          .score(propagated.score())
          .id(propagated.id())
          .build();
    }

    private ContactsRefreshEvent contactsRefreshEvent(int numRemaining, int numExpired) {
      return ContactsRefreshEvent.builder()
          .user(userName)
          .numRemaining(numRemaining)
          .numExpired(numExpired)
          .build();
    }

    private CurrentRefreshEvent currentRefreshEvent(RiskScoreMsg previous, RiskScoreMsg current) {
      return CurrentRefreshEvent.builder()
          .user(userName)
          .oldScore(previous.score())
          .newScore(current.score())
          .oldId(previous.id())
          .newId(current.id())
          .build();
    }

    private TimeoutEvent timeoutEvent() {
      return TimeoutEvent.builder().user(userName).build();
    }
  }
}

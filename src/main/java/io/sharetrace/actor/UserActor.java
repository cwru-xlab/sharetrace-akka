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
import io.sharetrace.logging.Logging;
import io.sharetrace.message.AlgorithmMsg;
import io.sharetrace.message.ContactMsg;
import io.sharetrace.message.ContactsRefreshMsg;
import io.sharetrace.message.CurrentRefreshMsg;
import io.sharetrace.message.ResumedMsg;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.message.ThresholdMsg;
import io.sharetrace.message.TimedOutMsg;
import io.sharetrace.message.UserMsg;
import io.sharetrace.model.UserParams;
import io.sharetrace.util.IntervalCache;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.immutables.builder.Builder;

/**
 * An actor that corresponds to a vertex in a {@link ContactNetwork}. Collectively, all {@link
 * UserActor}s carry out the execution of {@link RiskPropagation}.
 *
 * @see RiskPropagation
 * @see RiskScoreMsg
 * @see ContactMsg
 * @see TimedOutMsg
 * @see ContactsRefreshMsg
 * @see CurrentRefreshMsg
 * @see UserParams
 * @see IntervalCache
 */
public final class UserActor extends AbstractBehavior<UserMsg> {

  private final int timeoutId;
  private final ActorRef<AlgorithmMsg> riskProp;
  private final TimerScheduler<UserMsg> timers;
  private final UserLogger logger;
  private final UserParams userParams;
  private final Clock clock;
  private final IntervalCache<RiskScoreMsg> cache;
  private final Map<ActorRef<?>, ContactActor> contacts;
  private final MsgUtil msgUtil;
  private final RiskScoreMsg defaultMsg;
  private boolean timedOut;
  private RiskScoreMsg current;
  private RiskScoreMsg transmitted;

  private UserActor(
      ActorContext<UserMsg> ctx,
      ActorRef<AlgorithmMsg> riskProp,
      int timeoutId,
      TimerScheduler<UserMsg> timers,
      Set<Class<? extends Loggable>> loggable,
      UserParams userParams,
      Clock clock,
      IntervalCache<RiskScoreMsg> cache) {
    super(ctx);
    this.timeoutId = timeoutId;
    this.riskProp = riskProp;
    this.timers = timers;
    this.logger = new UserLogger(loggable, getContext());
    this.userParams = userParams;
    this.clock = clock;
    this.cache = cache;
    this.contacts = new Object2ObjectOpenHashMap<>();
    this.msgUtil = new MsgUtil(getContext(), clock, userParams);
    this.defaultMsg = msgUtil.defaultMsg();
    this.timedOut = false;
    updateCurrent(defaultMsg);
  }

  private RiskScoreMsg updateCurrent(RiskScoreMsg msg) {
    RiskScoreMsg previous = current;
    current = msg;
    transmitted = msgUtil.transmitted(current);
    return previous;
  }

  @Builder.Factory
  static Behavior<UserMsg> user(
      ActorRef<AlgorithmMsg> riskProp,
      int timeoutId,
      Map<String, String> mdc,
      Set<Class<? extends Loggable>> loggable,
      UserParams userParams,
      Clock clock,
      IntervalCache<RiskScoreMsg> cache) {
    return Behaviors.setup(
        ctx -> {
          ctx.setLoggerName(Logging.EVENTS_LOGGER_NAME);
          Behavior<UserMsg> user =
              Behaviors.withTimers(
                  timers ->
                      new UserActor(
                          ctx, riskProp, timeoutId, timers, loggable, userParams, clock, cache));
          return Behaviors.withMdc(UserMsg.class, msg -> mdc, user);
        });
  }

  @Override
  public Receive<UserMsg> createReceive() {
    return newReceiveBuilder()
        .onMessage(ContactMsg.class, this::handle)
        .onMessage(RiskScoreMsg.class, this::handle)
        .onMessage(CurrentRefreshMsg.class, this::handle)
        .onMessage(ContactsRefreshMsg.class, this::handle)
        .onMessage(ThresholdMsg.class, this::handle)
        .onMessage(TimedOutMsg.class, this::handle)
        .build();
  }

  private Behavior<UserMsg> handle(ContactMsg msg) {
    if (msgUtil.isAlive(msg)) {
      ContactActor contact = addNewContact(msg);
      startContactsRefreshTimer();
      logger.logContact(contact.ref());
      sendCurrentOrCached(contact);
    }
    return this;
  }

  private Behavior<UserMsg> handle(RiskScoreMsg msg) {
    resumeIfTimedOut();
    logger.logReceive(msg);
    RiskScoreMsg propagate = updateIfAboveCurrent(msg);
    cache.put(msg.score().time(), propagate);
    propagate(propagate);
    resetTimeout();
    return this;
  }

  private Behavior<UserMsg> handle(CurrentRefreshMsg msg) {
    RiskScoreMsg maxCachedOrDefault = cache.max(clock.instant()).orElse(defaultMsg);
    RiskScoreMsg previous = updateCurrent(maxCachedOrDefault);
    startCurrentRefreshTimer();
    logger.logCurrentRefresh(previous, current);
    return this;
  }

  private Behavior<UserMsg> handle(ContactsRefreshMsg msg) {
    int numContacts = contacts.size();
    contacts.values().removeIf(Predicate.not(ContactActor::isAlive));
    int numRemaining = contacts.size();
    int numExpired = numContacts - numRemaining;
    logger.logContactsRefresh(numRemaining, numExpired);
    startContactsRefreshTimer();
    return this;
  }

  private Behavior<UserMsg> handle(ThresholdMsg msg) {
    contacts.get(msg.contact()).updateThreshold();
    return this;
  }

  private Behavior<UserMsg> handle(TimedOutMsg msg) {
    timedOut = true;
    logger.logTimeout();
    riskProp.tell(msg);
    return this;
  }

  private ContactActor addNewContact(ContactMsg msg) {
    ContactActor contact = new ContactActor(msg, timers, msgUtil, cache);
    contacts.put(contact.ref(), contact);
    return contact;
  }

  private void startContactsRefreshTimer() {
    Duration minTtl = Collections.min(contacts.values()).ttl();
    timers.startSingleTimer(ContactsRefreshMsg.INSTANCE, minTtl);
  }

  private void sendCurrentOrCached(ContactActor contact) {
    if (contact.shouldReceive(current)) {
      sendCurrent(contact);
    } else {
      sendCached(contact);
    }
  }

  private void resumeIfTimedOut() {
    if (timedOut) {
      timedOut = false;
      logger.logResume();
      riskProp.tell(ResumedMsg.of(timeoutId));
    }
  }

  private RiskScoreMsg updateIfAboveCurrent(RiskScoreMsg msg) {
    RiskScoreMsg propagate;
    if (msgUtil.isGreaterThan(msg, current)) {
      RiskScoreMsg previous = updateCurrent(msg);
      startCurrentRefreshTimer();
      logger.logUpdate(previous, current);
      propagate = transmitted;
    } else {
      propagate = msgUtil.transmitted(msg);
    }
    return propagate;
  }

  private void propagate(RiskScoreMsg msg) {
    contacts.values().stream()
        .filter(contact -> contact.shouldReceive(msg))
        .forEach(contact -> contact.tell(msg, logger::logPropagate));
  }

  private void resetTimeout() {
    timers.startSingleTimer(TimedOutMsg.of(timeoutId), userParams.idleTimeout());
  }

  private void startCurrentRefreshTimer() {
    timers.startSingleTimer(CurrentRefreshMsg.INSTANCE, msgUtil.computeTtl(current));
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
}

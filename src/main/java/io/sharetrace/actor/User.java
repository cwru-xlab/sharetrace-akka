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
import io.sharetrace.message.ContactMsg;
import io.sharetrace.message.RefreshMsg;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.message.ThresholdMsg;
import io.sharetrace.message.TimeoutMsg;
import io.sharetrace.message.UserMsg;
import io.sharetrace.model.MsgParams;
import io.sharetrace.model.UserParams;
import io.sharetrace.util.IntervalCache;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
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
  private final UserLogger logger;
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
    this.logger = new UserLogger(loggable, getContext());
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
      contacts.put(contact.ref(), contact);
      logger.logContact(contact.ref());
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
    contacts.values().stream()
        .filter(contact -> contact.shouldReceive(msg))
        .forEach(contact -> contact.tell(msg, logger::logPropagate));
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
}

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
import io.sharetrace.message.ContactsRefreshMsg;
import io.sharetrace.message.CurrentRefreshMsg;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.message.ThresholdMsg;
import io.sharetrace.message.TimeoutMsg;
import io.sharetrace.message.UserMsg;
import io.sharetrace.model.MsgParams;
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
 * @see TimeoutMsg
 * @see ContactsRefreshMsg
 * @see CurrentRefreshMsg
 * @see UserParams
 * @see MsgParams
 * @see IntervalCache
 */
public final class UserActor extends AbstractBehavior<UserMsg> {

  private final TimerScheduler<UserMsg> timers;
  private final UserLogger logger;
  private final UserParams userParams;
  private final Clock clock;
  private final IntervalCache<RiskScoreMsg> cache;
  private final Map<ActorRef<?>, ContactActor> contacts;
  private final MsgUtil msgUtil;
  private final RiskScoreMsg defaultMsg;
  private RiskScoreMsg current;
  private RiskScoreMsg transmitted;

  private UserActor(
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
                  timers ->
                      new UserActor(ctx, timers, loggable, userParams, msgParams, clock, cache));
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
        .onMessage(TimeoutMsg.class, this::handle)
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
    logger.logReceive(msg);
    RiskScoreMsg propagate = updateIfIsAboveCurrent(msg);
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

  @SuppressWarnings("unused")
  private Behavior<UserMsg> handle(TimeoutMsg msg) {
    logger.logTimeout();
    return Behaviors.stopped();
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

  private RiskScoreMsg updateIfIsAboveCurrent(RiskScoreMsg msg) {
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
    timers.startSingleTimer(TimeoutMsg.INSTANCE, userParams.idleTimeout());
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

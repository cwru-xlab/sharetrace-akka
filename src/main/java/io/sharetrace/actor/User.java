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
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
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
  private final Map<ActorRef<UserMsg>, ContactInfo> contacts;
  private final UserParams userParams;
  private final MsgParams msgParams;
  private final Clock clock;
  private final IntervalCache<RiskScoreMsg> cache;
  private RiskScoreMsg current;
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
    this.logger = new EventLogger(loggable, getContext());
    this.contacts = new Object2ObjectOpenHashMap<>();
    this.userParams = userParams;
    this.msgParams = msgParams;
    this.clock = clock;
    this.cache = cache;
    this.current = defaultMsg();
    updateWith(current);
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

  private static boolean isFrom(ActorRef<?> contact, RiskScoreMsg received) {
    return contact.equals(received.replyTo());
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
    return newScoreMsgBuilder().score(RiskScore.MIN).build();
  }

  private void updateWith(RiskScoreMsg msg) {
    current = msg;
    transmitted = transmitted(current);
    sendThresh = current.scaledValue(msgParams.sendCoeff());
  }

  private RiskScoreMsg transmitted(RiskScoreMsg msg) {
    return newScoreMsgBuilder().score(transmitted(msg.score())).id(msg.id()).build();
  }

  private RiskScoreMsg.Builder newScoreMsgBuilder() {
    return RiskScoreMsg.builder()
        .clock(clock)
        .scoreTtl(msgParams.scoreTtl())
        .replyTo(getContext().getSelf());
  }

  private RiskScore transmitted(RiskScore score) {
    return score.withValue(score.value() * msgParams.transRate());
  }

  private void startRefreshTimer() {
    timers.startTimerWithFixedDelay(RefreshMsg.INSTANCE, userParams.refreshPeriod());
  }

  private Behavior<UserMsg> onContactMsg(ContactMsg msg) {
    if (msg.isAlive()) {
      ActorRef<UserMsg> contact = msg.contact();
      ContactInfo info = new ContactInfo(msg, msgParams);
      contacts.put(contact, info);
      logger.logContact(contact);
      sendToContact(contact, info);
    }
    return this;
  }

  private void sendToContact(ActorRef<UserMsg> contact, ContactInfo info) {
    if (current.isAlive() && !current.isAfter(info.contactTime)) {
      sendCurrent(contact);
    } else {
      sendCached(contact, info.contactTime);
    }
  }

  private void sendCurrent(ActorRef<UserMsg> contact) {
    tell(contact, transmitted, logger::logSendCurrent);
  }

  private void sendCached(ActorRef<UserMsg> contact, Instant contactTime) {
    cache
        .max(contactTime)
        .filter(RiskScoreMsg::isAlive)
        .ifPresent(cached -> tell(contact, cached, logger::logSendCached));
  }

  private Behavior<UserMsg> onRiskScoreMsg(RiskScoreMsg received) {
    logger.logReceive(received);
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
    logger.logTimeout();
    return Behaviors.stopped();
  }

  private void resetTimeout() {
    timers.startSingleTimer(TimeoutMsg.INSTANCE, userParams.idleTimeout());
  }

  private void refreshContacts() {
    int numContacts = contacts.size();
    contacts.values().removeIf(Predicate.not(ContactInfo::isAlive));
    int numRemaining = contacts.size();
    logger.logContactsRefresh(numRemaining, numContacts - numRemaining);
  }

  private void refreshCurrent() {
    if (!current.isAlive()) {
      RiskScoreMsg previous = current;
      updateWith(cache.max(clock.instant()).orElseGet(this::defaultMsg));
      logger.logCurrentRefresh(previous, current);
    }
  }

  private boolean isContactRecent(ContactInfo contactInfo, RiskScoreMsg relativeTo) {
    return !relativeTo.isAfter(contactInfo.contactTime);
  }

  private void tell(
      ActorRef<UserMsg> contact, RiskScoreMsg msg, BiConsumer<ActorRef<?>, RiskScoreMsg> log) {
    contact.tell(msg);
    contacts.get(contact).lastSent = msg.score().value();
    log.accept(contact, msg);
  }

  private void propagate(RiskScoreMsg msg) {
    if (msg.isAlive()) {
      contacts.entrySet().stream()
          .filter(entry -> shouldPropagateTo(entry, msg))
          .map(Entry::getKey)
          .forEach(contact -> tell(contact, msg, logger::logPropagate));
    }
  }

  private boolean shouldPropagateTo(Entry<ActorRef<UserMsg>, ContactInfo> entry, RiskScoreMsg msg) {
    ActorRef<?> contact = entry.getKey();
    ContactInfo info = entry.getValue();
    return !isFrom(contact, msg) && isContactRecent(info, msg) && isHighEnough(msg, info);
  }

  private boolean isHighEnough(RiskScoreMsg msg) {
    return msg.isGreaterThan(sendThresh);
  }

  private boolean isHighEnough(RiskScoreMsg msg, ContactInfo contactInfo) {
    return msg.isGreaterThan(contactInfo.lastSent + msgParams.tolerance());
  }

  private RiskScoreMsg updateAndCache(RiskScoreMsg msg) {
    RiskScoreMsg propagate;
    if (msg.isGreaterThan(current)) {
      RiskScoreMsg previous = current;
      updateWith(msg);
      logger.logUpdate(previous, current);
      propagate = transmitted;
    } else {
      propagate = transmitted(msg);
    }
    cache.put(msg.score().time(), propagate);
    return propagate;
  }

  private static final class ContactInfo {

    private final ContactMsg msg;
    private final Instant contactTime;
    private float lastSent;

    public ContactInfo(ContactMsg msg, MsgParams msgParams) {
      this.msg = msg;
      this.contactTime = msg.contactTime().plus(msgParams.timeBuffer());
      this.lastSent = RiskScore.MIN_VALUE;
    }

    public boolean isAlive() {
      return msg.isAlive();
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

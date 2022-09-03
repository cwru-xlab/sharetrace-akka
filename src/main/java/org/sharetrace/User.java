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
import org.sharetrace.logging.Loggables;
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
import org.sharetrace.message.ContactMessage;
import org.sharetrace.message.RefreshMessage;
import org.sharetrace.message.RiskScore;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.message.TimeoutMessage;
import org.sharetrace.message.UserMessage;
import org.sharetrace.message.UserParameters;
import org.sharetrace.util.IntervalCache;
import org.sharetrace.util.TypedSupplier;

/**
 * An actor that corresponds to a {@link ContactNetwork} node. Collectively, all {@link User}s carry
 * out the execution of {@link RiskPropagation}.
 *
 * @see UserParameters
 */
public class User extends AbstractBehavior<UserMessage> {

  private final TimerScheduler<UserMessage> timers;
  private final Loggables loggables;
  private final Map<ActorRef<UserMessage>, Instant> contacts;
  private final UserParameters parameters;
  private final Clock clock;
  private final IntervalCache<RiskScoreMessage> cache;
  private RiskScoreMessage previous;
  private RiskScoreMessage current;
  private RiskScoreMessage transmitted;
  private float sendThreshold;

  private User(
      ActorContext<UserMessage> context,
      TimerScheduler<UserMessage> timers,
      Set<Class<? extends Loggable>> loggable,
      UserParameters parameters,
      Clock clock,
      IntervalCache<RiskScoreMessage> cache) {
    super(context);
    this.timers = timers;
    this.loggables = Loggables.create(loggable, () -> getContext().getLog());
    this.contacts = new Object2ObjectOpenHashMap<>();
    this.parameters = parameters;
    this.clock = clock;
    this.cache = cache;
    this.current = defaultMessage();
    setMessagesAndThreshold(current);
    startRefreshTimer();
  }

  private RiskScoreMessage defaultMessage() {
    return RiskScoreMessage.builder()
        .score(RiskScore.ofMinValue(clock.instant()))
        .replyTo(getContext().getSelf())
        .build();
  }

  private void setMessagesAndThreshold(RiskScoreMessage newCurrent) {
    previous = current;
    current = newCurrent;
    transmitted = transmitted(current);
    sendThreshold = current.score().value() * parameters.sendCoefficient();
  }

  private void startRefreshTimer() {
    timers.startTimerWithFixedDelay(RefreshMessage.INSTANCE, parameters.refreshPeriod());
  }

  private RiskScoreMessage transmitted(RiskScoreMessage received) {
    return RiskScoreMessage.builder()
        .replyTo(getContext().getSelf())
        .score(transmittedScore(received))
        .uuid(received.uuid())
        .build();
  }

  private RiskScore transmittedScore(RiskScoreMessage received) {
    return RiskScore.builder()
        .value(received.score().value() * parameters.transmissionRate())
        .timestamp(received.score().timestamp())
        .build();
  }

  @Builder.Factory
  static Behavior<UserMessage> user(
      Map<String, String> mdc,
      Set<Class<? extends Loggable>> loggable,
      UserParameters parameters,
      Clock clock,
      IntervalCache<RiskScoreMessage> cache) {
    return Behaviors.setup(
        context -> {
          context.setLoggerName(Logging.EVENT_LOGGER_NAME);
          Behavior<UserMessage> user =
              Behaviors.withTimers(
                  timers -> new User(context, timers, loggable, parameters, clock, cache));
          return Behaviors.withMdc(UserMessage.class, message -> mdc, user);
        });
  }

  private static Predicate<Entry<ActorRef<UserMessage>, Instant>> isNotFrom(
      RiskScoreMessage message) {
    return Predicate.not(contact -> contact.getKey().equals(message.replyTo()));
  }

  private static SendCurrentEvent sendCurrentEvent(
      ActorRef<UserMessage> contact, RiskScoreMessage message) {
    return SendCurrentEvent.builder()
        .from(name(message.replyTo()))
        .to(name(contact))
        .score(message.score())
        .uuid(message.uuid())
        .build();
  }

  private static SendCachedEvent sendCachedEvent(
      ActorRef<UserMessage> contact, RiskScoreMessage message) {
    return SendCachedEvent.builder()
        .from(name(message.replyTo()))
        .to(name(contact))
        .score(message.score())
        .uuid(message.uuid())
        .build();
  }

  private static PropagateEvent propagateEvent(
      ActorRef<UserMessage> contact, RiskScoreMessage message) {
    return PropagateEvent.builder()
        .from(name(message.replyTo()))
        .to(name(contact))
        .score(message.score())
        .uuid(message.uuid())
        .build();
  }

  private static String name(ActorRef<UserMessage> user) {
    return user.path().name();
  }

  @Override
  public Receive<UserMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(ContactMessage.class, this::onContactMessage)
        .onMessage(RiskScoreMessage.class, this::onRiskScoreMessage)
        .onMessage(TimeoutMessage.class, x -> Behaviors.stopped())
        .onMessage(RefreshMessage.class, this::onRefreshMessage)
        .build();
  }

  private Behavior<UserMessage> onContactMessage(ContactMessage message) {
    addContactIfAlive(message);
    sendToContact(message);
    return this;
  }

  private void sendToContact(ContactMessage message) {
    if (isScoreAlive(current) && isContactRecent(message, current)) {
      sendCurrent(message);
    } else {
      sendCached(message);
    }
  }

  private Behavior<UserMessage> onRiskScoreMessage(RiskScoreMessage message) {
    logReceive(message);
    propagate(updateAndCache(message));
    resetTimeout();
    return this;
  }

  private Behavior<UserMessage> onRefreshMessage(RefreshMessage message) {
    refreshContacts();
    refreshCurrent();
    return this;
  }

  private void sendCurrent(ContactMessage message) {
    ActorRef<UserMessage> contact = message.replyTo();
    contact.tell(transmitted);
    logSendCurrent(contact);
  }

  private void resetTimeout() {
    timers.startSingleTimer(TimeoutMessage.INSTANCE, parameters.idleTimeout());
  }

  private void logSendCurrent(ActorRef<UserMessage> contact) {
    logEvent(SendCurrentEvent.class, () -> sendCurrentEvent(contact, transmitted));
  }

  private void refreshContacts() {
    int nContacts = contacts.size();
    contacts.values().removeIf(Predicate.not(this::isContactAlive));
    int nRemaining = contacts.size();
    int nExpired = nContacts - nRemaining;
    logContactsRefresh(nRemaining, nExpired);
  }

  private void refreshCurrent() {
    if (!isScoreAlive(current)) {
      setMessagesAndThreshold(maxCachedOrDefault());
      logCurrentRefresh();
    }
  }

  private RiskScoreMessage maxCachedOrDefault() {
    return cache.max(clock.instant()).orElseGet(this::defaultMessage);
  }

  private void addContactIfAlive(ContactMessage message) {
    if (isContactAlive(message.timestamp())) {
      contacts.put(message.replyTo(), message.timestamp());
      logContact(message);
    }
  }

  private void sendCached(ContactMessage message) {
    Instant buffered = buffered(message.timestamp());
    cache.max(buffered).ifPresent(cached -> sendCached(message.replyTo(), cached));
  }

  private Predicate<Entry<ActorRef<UserMessage>, Instant>> isContactRecent(
      RiskScoreMessage message) {
    return contact -> isRecent(contact.getValue(), message);
  }

  private void sendCached(ActorRef<UserMessage> contact, RiskScoreMessage cached) {
    if (isScoreAlive(cached)) {
      contact.tell(cached);
      logSendCached(contact, cached);
    }
  }

  private void propagate(RiskScoreMessage message) {
    if (isScoreAlive(message) && isHighEnough(message)) {
      contacts.entrySet().stream()
          .filter(isNotFrom(message))
          .filter(isContactRecent(message))
          .map(Entry::getKey)
          .forEach(contact -> propagate(contact, message));
    }
  }

  private boolean isHighEnough(RiskScoreMessage message) {
    return message.score().value() >= sendThreshold;
  }

  private void logSendCached(ActorRef<UserMessage> contact, RiskScoreMessage message) {
    logEvent(SendCachedEvent.class, () -> sendCachedEvent(contact, message));
  }

  private boolean isContactRecent(ContactMessage contact, RiskScoreMessage message) {
    return isRecent(contact.timestamp(), message);
  }

  private boolean isRecent(Instant timestamp, RiskScoreMessage message) {
    // Message timestamp is no newer (inclusive) than the buffered timestamp.
    return !message.score().timestamp().isAfter(buffered(timestamp));
  }

  private Instant buffered(Instant timestamp) {
    return timestamp.plus(parameters.timeBuffer());
  }

  private boolean isContactAlive(Instant timestamp) {
    return isAlive(timestamp, parameters.contactTtl());
  }

  private boolean isScoreAlive(RiskScoreMessage message) {
    return isAlive(message.score().timestamp(), parameters.scoreTtl());
  }

  private void propagate(ActorRef<UserMessage> contact, RiskScoreMessage message) {
    contact.tell(message);
    logPropagate(contact, message);
  }

  private void logPropagate(ActorRef<UserMessage> contact, RiskScoreMessage message) {
    logEvent(PropagateEvent.class, () -> propagateEvent(contact, message));
  }

  private void logContact(ContactMessage message) {
    logEvent(ContactEvent.class, () -> contactEvent(message));
  }

  private void logContactsRefresh(int nRemaining, int nExpired) {
    logEvent(ContactsRefreshEvent.class, () -> contactsRefreshEvent(nRemaining, nExpired));
  }

  private ContactsRefreshEvent contactsRefreshEvent(int nRemaining, int nExpired) {
    return ContactsRefreshEvent.builder()
        .of(name())
        .nRemaining(nRemaining)
        .nExpired(nExpired)
        .build();
  }

  private void logCurrentRefresh() {
    logEvent(CurrentRefreshEvent.class, this::currentRefreshEvent);
  }

  private CurrentRefreshEvent currentRefreshEvent() {
    return CurrentRefreshEvent.builder()
        .of(name())
        .oldScore(previous.score())
        .newScore(current.score())
        .oldUuid(previous.uuid())
        .newUuid(current.uuid())
        .build();
  }

  private RiskScoreMessage updateAndCache(RiskScoreMessage message) {
    RiskScoreMessage propagate;
    if (isHigherThanCurrent(message)) {
      setMessagesAndThreshold(message);
      logUpdate();
      propagate = transmitted;
    } else {
      propagate = transmitted(message);
    }
    return cached(propagate);
  }

  private boolean isHigherThanCurrent(RiskScoreMessage message) {
    // Do NOT use scoreTolerance; otherwise, it causes issues with logging and analysis.
    return message.score().value() > current.score().value();
  }

  private RiskScoreMessage cached(RiskScoreMessage message) {
    cache.put(message.score().timestamp(), message);
    return message;
  }

  private void logUpdate() {
    logEvent(UpdateEvent.class, this::updateEvent);
  }

  private <T extends Loggable> void logEvent(Class<T> type, Supplier<T> supplier) {
    loggables.log(LoggableEvent.KEY, TypedSupplier.of(type, supplier));
  }

  private UpdateEvent updateEvent() {
    return UpdateEvent.builder()
        .from(name(current.replyTo()))
        .to(name())
        .oldScore(previous.score())
        .newScore(current.score())
        .oldUuid(previous.uuid())
        .newUuid(current.uuid())
        .build();
  }

  private String name() {
    return name(getContext().getSelf());
  }

  private ContactEvent contactEvent(ContactMessage message) {
    return ContactEvent.builder().of(name()).addUsers(name(), name(message.replyTo())).build();
  }

  private void logReceive(RiskScoreMessage message) {
    logEvent(ReceiveEvent.class, () -> receiveEvent(message));
  }

  private ReceiveEvent receiveEvent(RiskScoreMessage message) {
    return ReceiveEvent.builder()
        .from(name(message.replyTo()))
        .to(name())
        .score(message.score())
        .uuid(message.uuid())
        .build();
  }

  private boolean isAlive(Instant timestamp, Duration timeToLive) {
    return Duration.between(timestamp, clock.instant()).compareTo(timeToLive) < 0;
  }
}

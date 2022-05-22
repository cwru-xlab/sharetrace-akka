package org.sharetrace.graph;

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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.immutables.builder.Builder;
import org.sharetrace.RiskPropagation;
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
import org.sharetrace.message.NodeMessage;
import org.sharetrace.message.NodeParameters;
import org.sharetrace.message.Refresh;
import org.sharetrace.message.RiskScore;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.message.Timeout;
import org.sharetrace.util.IntervalCache;
import org.sharetrace.util.TypedSupplier;

/**
 * An actor that corresponds to a {@link ContactGraph} node. Collectively, all {@link Node}s carry
 * out the execution of {@link RiskPropagation}.
 *
 * @see NodeParameters
 * @see IntervalCache
 */
public class Node extends AbstractBehavior<NodeMessage> {

  private final TimerScheduler<NodeMessage> timers;
  private final Loggables loggables;
  private final Map<ActorRef<NodeMessage>, Instant> contacts;
  private final NodeParameters parameters;
  private final Clock clock;
  private final IntervalCache<RiskScoreMessage> cache;
  private RiskScoreMessage previous;
  private RiskScoreMessage current;
  private double sendThreshold;

  private Node(
      ActorContext<NodeMessage> context,
      TimerScheduler<NodeMessage> timers,
      Set<Class<? extends Loggable>> loggable,
      NodeParameters parameters,
      Clock clock,
      IntervalCache<RiskScoreMessage> cache) {
    super(context);
    this.timers = timers;
    this.loggables = Loggables.create(loggable, () -> getContext().getLog());
    this.contacts = new Object2ObjectOpenHashMap<>();
    this.parameters = parameters;
    this.clock = clock;
    this.cache = cache;
    this.previous = cached(defaultMessage());
    this.current = previous;
    setSendThreshold();
    startRefreshTimer();
  }

  private RiskScoreMessage cached(RiskScoreMessage message) {
    cache.put(message.score().timestamp(), message);
    return message;
  }

  private RiskScoreMessage defaultMessage() {
    return RiskScoreMessage.builder()
        .score(RiskScore.of(RiskScore.MIN_VALUE, clock.instant()))
        .replyTo(getContext().getSelf())
        .build();
  }

  private void setSendThreshold() {
    sendThreshold = current.score().value() * parameters.transmissionRate();
  }

  private void startRefreshTimer() {
    timers.startTimerWithFixedDelay(Refresh.INSTANCE, parameters.refreshRate());
  }

  @Builder.Factory
  static Behavior<NodeMessage> node(
      TimerScheduler<NodeMessage> timers,
      Set<Class<? extends Loggable>> loggable,
      NodeParameters parameters,
      Clock clock,
      IntervalCache<RiskScoreMessage> cache) {
    return Behaviors.setup(
        context -> {
          context.setLoggerName(Logging.EVENT_LOGGER_NAME);
          return new Node(context, timers, loggable, parameters, clock, cache);
        });
  }

  private static Predicate<Entry<ActorRef<NodeMessage>, Instant>> isNotFrom(
      RiskScoreMessage message) {
    return Predicate.not(contact -> Objects.equals(contact.getKey(), message.replyTo()));
  }

  private static Behavior<NodeMessage> onTimeout(Timeout timeout) {
    return Behaviors.stopped();
  }

  private static SendCurrentEvent sendCurrentEvent(
      ActorRef<NodeMessage> contact, RiskScoreMessage message) {
    return SendCurrentEvent.builder()
        .from(name(message.replyTo()))
        .to(name(contact))
        .score(message.score())
        .uuid(message.uuid())
        .build();
  }

  private static SendCachedEvent sendCachedEvent(
      ActorRef<NodeMessage> contact, RiskScoreMessage message) {
    return SendCachedEvent.builder()
        .from(name(message.replyTo()))
        .to(name(contact))
        .score(message.score())
        .uuid(message.uuid())
        .build();
  }

  private static String name(ActorRef<NodeMessage> node) {
    return node.path().name();
  }

  private static PropagateEvent propagateEvent(
      ActorRef<NodeMessage> contact, RiskScoreMessage message) {
    return PropagateEvent.builder()
        .from(name(message.replyTo()))
        .to(name(contact))
        .score(message.score())
        .uuid(message.uuid())
        .build();
  }

  @Override
  public Receive<NodeMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(ContactMessage.class, this::onContactMessage)
        .onMessage(RiskScoreMessage.class, this::onRiskScoreMessage)
        .onMessage(Timeout.class, Node::onTimeout)
        .onMessage(Refresh.class, this::onRefresh)
        .build();
  }

  private void resetTimeout() {
    timers.startSingleTimer(Timeout.INSTANCE, parameters.idleTimeout());
  }

  private Behavior<NodeMessage> onRefresh(Refresh refresh) {
    refreshContacts();
    refreshCurrent();
    return this;
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
      RiskScoreMessage newCurrent = maxCachedOrDefault(clock.instant());
      updatePreviousAndCurrent(newCurrent);
      logCurrentRefresh();
    }
  }

  private Optional<RiskScoreMessage> maxCached(Instant timestamp) {
    return cache.headMax(timestamp, RiskScoreMessage::compareTo);
  }

  private RiskScoreMessage maxCachedOrDefault(Instant timestamp) {
    return cache.headMax(timestamp, RiskScoreMessage::compareTo).orElseGet(this::defaultMessage);
  }

  private Behavior<NodeMessage> onContactMessage(ContactMessage message) {
    if (isContactAlive(message)) {
      addContact(message);
      if (!sendCurrent(message)) {
        sendCached(message);
      }
    }
    return this;
  }

  private void addContact(ContactMessage message) {
    contacts.put(message.replyTo(), message.timestamp());
    logContact(message);
  }

  private boolean sendCurrent(ContactMessage message) {
    boolean sent = isScoreAlive(current) && isContactRecent(message, current);
    if (sent) {
      ActorRef<NodeMessage> contact = message.replyTo();
      RiskScoreMessage transmitted = transmitted(current);
      contact.tell(transmitted);
      logSendCurrent(contact, transmitted);
    }
    return sent;
  }

  private void sendCached(ContactMessage message) {
    Instant buffered = buffered(message.timestamp());
    maxCached(buffered).ifPresent(cached -> sendCached(message.replyTo(), cached));
  }

  private void sendCached(ActorRef<NodeMessage> contact, RiskScoreMessage cached) {
    RiskScoreMessage transmitted = transmitted(cached);
    contact.tell(transmitted);
    logSendCached(contact, transmitted);
  }

  private Behavior<NodeMessage> onRiskScoreMessage(RiskScoreMessage message) {
    logReceive(message);
    update(message);
    propagate(message);
    resetTimeout();
    return this;
  }

  private void update(RiskScoreMessage message) {
    if (cached(message).score().value() > current.score().value()) {
      updatePreviousAndCurrent(message);
      setSendThreshold();
      logUpdate();
    }
  }

  private void updatePreviousAndCurrent(RiskScoreMessage newCurrent) {
    previous = current;
    current = newCurrent;
  }

  private void propagate(RiskScoreMessage message) {
    RiskScoreMessage transmitted = transmitted(message);
    if (isScoreAlive(transmitted) && isHighEnough(transmitted)) {
      contacts.entrySet().stream()
          .filter(isNotFrom(message))
          .filter(isContactRecent(message))
          .forEach(contact -> propagate(contact.getKey(), transmitted));
    }
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

  private boolean isHighEnough(RiskScoreMessage message) {
    return message.score().value() >= sendThreshold;
  }

  private Predicate<Entry<ActorRef<NodeMessage>, Instant>> isContactRecent(
      RiskScoreMessage message) {
    return contact -> isRecent(contact.getValue(), message);
  }

  private boolean isContactRecent(ContactMessage contact, RiskScoreMessage message) {
    return isRecent(contact.timestamp(), message);
  }

  private boolean isRecent(Instant timestamp, RiskScoreMessage message) {
    return buffered(timestamp).isAfter(message.score().timestamp());
  }

  private Instant buffered(Instant timestamp) {
    return timestamp.plus(parameters.timeBuffer());
  }

  private boolean isContactAlive(ContactMessage message) {
    return isContactAlive(message.timestamp());
  }

  private boolean isContactAlive(Instant timestamp) {
    return isAlive(timestamp, parameters.contactTtl());
  }

  private boolean isScoreAlive(RiskScoreMessage message) {
    return isAlive(message.score().timestamp(), parameters.scoreTtl());
  }

  private boolean isAlive(Instant timestamp, Duration timeToLive) {
    Duration sinceTimestamp = Duration.between(timestamp, clock.instant());
    return timeToLive.compareTo(sinceTimestamp) >= 0;
  }

  private void propagate(ActorRef<NodeMessage> contact, RiskScoreMessage message) {
    contact.tell(message);
    logPropagate(contact, message);
  }

  private <T extends Loggable> void logEvent(Class<T> type, Supplier<T> supplier) {
    loggables.info(LoggableEvent.KEY, TypedSupplier.of(type, supplier));
  }

  private ContactEvent contactEvent(ContactMessage message) {
    return ContactEvent.builder().of(name()).addNodes(name(), name(message.replyTo())).build();
  }

  private void logContact(ContactMessage message) {
    logEvent(ContactEvent.class, () -> contactEvent(message));
  }

  private void logUpdate() {
    logEvent(UpdateEvent.class, this::updateEvent);
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

  private String name() {
    return name(getContext().getSelf());
  }

  private void logSendCurrent(ActorRef<NodeMessage> contact, RiskScoreMessage message) {
    logEvent(SendCurrentEvent.class, () -> sendCurrentEvent(contact, message));
  }

  private void logSendCached(ActorRef<NodeMessage> contact, RiskScoreMessage message) {
    logEvent(SendCachedEvent.class, () -> sendCachedEvent(contact, message));
  }

  private void logPropagate(ActorRef<NodeMessage> contact, RiskScoreMessage message) {
    logEvent(PropagateEvent.class, () -> propagateEvent(contact, message));
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
}

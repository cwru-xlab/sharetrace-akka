package org.sharetrace.model.graph;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.immutables.builder.Builder;
import org.sharetrace.RiskPropagation;
import org.sharetrace.model.message.ContactMessage;
import org.sharetrace.model.message.NodeMessage;
import org.sharetrace.model.message.Parameters;
import org.sharetrace.model.message.Refresh;
import org.sharetrace.model.message.RiskScore;
import org.sharetrace.model.message.RiskScoreMessage;
import org.sharetrace.model.message.Timeout;
import org.sharetrace.util.IntervalCache;
import org.sharetrace.util.Log;
import org.sharetrace.util.Loggable;
import org.slf4j.Logger;

/**
 * An actor that corresponds to a {@link ContactGraph} node. Collectively, all {@link Node}s carry
 * out the execution of {@link RiskPropagation}.
 *
 * @see Parameters
 * @see IntervalCache
 */
public class Node extends AbstractBehavior<NodeMessage> {

  private static final String CONTACT_FORMAT =
      "{\"event\": \"addContact\", \"nodes\": [\"{}\", \"{}\"]}";
  private static final String RECEIVE_FORMAT =
      "{\"event\": \"receive\", \"from\": \"{}\", \"to\": \"{}\", \"value\": {}, \"timestamp\": \"{}\", \"uuid\": \"{}\"}";
  private static final String PROPAGATE_FORMAT =
      "{\"event\": \"propagate\", \"from\": \"{}\", \"to\": \"{}\", \"value\": {}, \"timestamp\": \"{}\", \"uuid\": \"{}\"}";
  private static final String SEND_CURRENT_FORMAT =
      "{\"event\": \"sendCurrent\", \"from\": \"{}\", \"to\": \"{}\", \"value\": {}, \"timestamp\": \"{}\", \"uuid\": \"{}\"}";
  private static final String SEND_CACHED_FORMAT =
      "{\"event\": \"sendCached\", \"from\": \"{}\", \"to\": \"{}\", \"value\": {}, \"timestamp\": \"{}\", \"uuid\": \"{}\"}";
  private static final String UPDATE_FORMAT =
      "{\"event\": \"update\", \"from\": \"{}\", \"to\": \"{}\", \"oldValue\": {}, \"newValue\": {}, \"oldTimestamp\": \"{}\", \"newTimestamp\": \"{}\", \"oldUuid\": \"{}\", \"newUuid\": \"{}\"}";
  private static final String CONTACTS_REFRESH_FORMAT =
      "{\"event\": \"contactsRefresh\", \"of\": \"{}\", \"nRemaining\": \"{}\", \"nExpired\": {}}";
  private static final String CURRENT_REFRESH_FORMAT =
      "{\"event\": \"currentRefresh\", \"of\": \"{}\", \"oldValue\": {}, \"newValue\": {}, \"oldTimestamp\": \"{}\", \"newTimestamp\": \"{}\", \"oldUuid\": \"{}\", \"newUuid\": \"{}\"}";

  private final TimerScheduler<NodeMessage> timers;
  private final Log log;
  private final Map<ActorRef<NodeMessage>, Instant> contacts;
  private final Parameters parameters;
  private final BiFunction<RiskScore, Parameters, RiskScore> transmitter;
  private final Supplier<Instant> clock;
  private final IntervalCache<RiskScoreMessage> cache;
  private RiskScoreMessage current;
  private double sendThreshold;

  private Node(
      ActorContext<NodeMessage> context,
      TimerScheduler<NodeMessage> timers,
      Log log,
      Parameters parameters,
      BiFunction<RiskScore, Parameters, RiskScore> transmitter,
      Supplier<Instant> clock,
      IntervalCache<RiskScoreMessage> cache) {
    super(context);
    this.timers = timers;
    this.log = log;
    this.contacts = new Object2ObjectOpenHashMap<>();
    this.parameters = parameters;
    this.transmitter = transmitter;
    this.clock = clock;
    this.cache = cache;
    this.current = cached(defaultMessage());
    setSendThreshold();
    startRefreshTimer();
  }

  @Builder.Factory
  protected static Behavior<NodeMessage> node(
      TimerScheduler<NodeMessage> timers,
      Log log,
      Parameters parameters,
      BiFunction<RiskScore, Parameters, RiskScore> transmitter,
      Supplier<Instant> clock,
      IntervalCache<RiskScoreMessage> cache) {
    return Behaviors.setup(
        context -> new Node(context, timers, log, parameters, transmitter, clock, cache));
  }

  private static Predicate<Entry<ActorRef<NodeMessage>, Instant>> isNotFrom(
      RiskScoreMessage message) {
    return Predicate.not(contact -> Objects.equals(contact.getKey(), message.replyTo()));
  }

  private static Behavior<NodeMessage> onTimeout(Timeout timeout) {
    return Behaviors.stopped();
  }

  private static String name(ActorRef<NodeMessage> node) {
    return node.path().name();
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
      RiskScoreMessage cached = maxCached(clock.get());
      RiskScoreMessage newValue = cached != null ? cached : defaultMessage();
      logCurrentRefresh(current, newValue);
      current = newValue;
    }
  }

  @Nullable
  private RiskScoreMessage maxCached(Instant timestamp) {
    return cache.headMax(timestamp, RiskScoreMessage::compareTo);
  }

  private RiskScoreMessage defaultMessage() {
    return RiskScoreMessage.builder()
        .score(RiskScore.MIN_VALUE, clock.get())
        .replyTo(self())
        .build();
  }

  private Behavior<NodeMessage> onContactMessage(ContactMessage message) {
    if (isContactAlive(message)) {
      logContact(message);
      addContact(message);
      sendToContact(message);
    }
    return this;
  }

  private void addContact(ContactMessage message) {
    contacts.put(message.replyTo(), message.timestamp());
  }

  private void sendToContact(ContactMessage message) {
    if (!sendCurrent(message)) {
      sendCached(message);
    }
  }

  private boolean sendCurrent(ContactMessage message) {
    boolean sent = isScoreAlive(current) && isContactRecent(message, current);
    if (sent) {
      ActorRef<NodeMessage> contact = message.replyTo();
      RiskScoreMessage transmitted = transmitted(current);
      logSendCurrent(contact, transmitted);
      contact.tell(transmitted);
    }
    return sent;
  }

  private void sendCached(ContactMessage message) {
    RiskScoreMessage cached = maxCached(buffered(message.timestamp()));
    if (cached != null) {
      ActorRef<NodeMessage> contact = message.replyTo();
      RiskScoreMessage transmitted = transmitted(cached);
      logSendCached(contact, transmitted);
      contact.tell(transmitted);
    }
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
      logUpdate(current, message);
      current = message;
      setSendThreshold();
    }
  }

  private RiskScoreMessage cached(RiskScoreMessage message) {
    cache.put(message.score().timestamp(), message);
    return message;
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
        .replyTo(self())
        .score(transmitter.apply(received.score(), parameters))
        .uuid(received.uuid())
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
    Duration sinceTimestamp = Duration.between(timestamp, clock.get());
    return timeToLive.compareTo(sinceTimestamp) >= 0;
  }

  // TODO Add as input? Implicitly depends on transmitter
  private void setSendThreshold() {
    sendThreshold = current.score().value() * parameters.transmissionRate();
  }

  private void resetTimeout() {
    timers.startSingleTimer(Timeout.INSTANCE, parameters.idleTimeout());
  }

  private void startRefreshTimer() {
    timers.startTimerWithFixedDelay(Refresh.INSTANCE, parameters.refreshRate());
  }

  private void propagate(ActorRef<NodeMessage> contact, RiskScoreMessage message) {
    if (log.contains(Loggable.PROPAGATE)) {
      logPropagate(contact, message);
    }
    contact.tell(message);
  }

  private void logContact(ContactMessage message) {
    if (log.contains(Loggable.NEW_CONTACT)) {
      log().debug(CONTACT_FORMAT, name(self()), name(message.replyTo()));
    }
  }

  private void logUpdate(RiskScoreMessage previous, RiskScoreMessage current) {
    if (log.contains(Loggable.UPDATE)) {
      RiskScore prev = previous.score();
      RiskScore curr = current.score();
      log()
          .debug(
              UPDATE_FORMAT,
              name(current.replyTo()),
              name(self()),
              prev.value(),
              curr.value(),
              prev.timestamp(),
              curr.timestamp(),
              previous.uuid(),
              current.uuid());
    }
  }

  private void logContactsRefresh(int nRemaining, int nExpired) {
    if (log.contains(Loggable.CONTACTS_REFRESH)) {
      log().debug(CONTACTS_REFRESH_FORMAT, name(self()), nRemaining, nExpired);
    }
  }

  private void logCurrentRefresh(RiskScoreMessage previous, RiskScoreMessage current) {
    if (log.contains(Loggable.CURRENT_REFRESH)) {
      RiskScore prev = previous.score();
      RiskScore curr = current.score();
      log()
          .debug(
              CURRENT_REFRESH_FORMAT,
              name(self()),
              prev.value(),
              curr.value(),
              prev.timestamp(),
              curr.timestamp(),
              previous.uuid(),
              current.uuid());
    }
  }

  private void logSendCurrent(ActorRef<NodeMessage> contact, RiskScoreMessage message) {
    if (log.contains(Loggable.SEND_CURRENT)) {
      logMessageOp(SEND_CURRENT_FORMAT, self(), contact, message);
    }
  }

  private void logSendCached(ActorRef<NodeMessage> contact, RiskScoreMessage message) {
    if (log.contains(Loggable.SEND_CACHED)) {
      logMessageOp(SEND_CACHED_FORMAT, self(), contact, message);
    }
  }

  private void logPropagate(ActorRef<NodeMessage> contact, RiskScoreMessage message) {
    if (log.contains(Loggable.PROPAGATE)) {
      logMessageOp(PROPAGATE_FORMAT, self(), contact, message);
    }
  }

  private void logReceive(RiskScoreMessage message) {
    if (log.contains(Loggable.RECEIVE)) {
      logMessageOp(RECEIVE_FORMAT, message.replyTo(), self(), message);
    }
  }

  private void logMessageOp(
      String format,
      ActorRef<NodeMessage> from,
      ActorRef<NodeMessage> to,
      RiskScoreMessage message) {
    RiskScore score = message.score();
    log().debug(format, name(from), name(to), score.value(), score.timestamp(), message.uuid());
  }

  private Logger log() {
    return getContext().getLog();
  }

  private ActorRef<NodeMessage> self() {
    return getContext().getSelf();
  }
}

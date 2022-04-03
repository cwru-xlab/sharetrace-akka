package org.sharetrace.model.graph;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.immutables.builder.Builder;
import org.sharetrace.model.message.Contact;
import org.sharetrace.model.message.NodeMessage;
import org.sharetrace.model.message.Parameters;
import org.sharetrace.model.message.RiskScore;
import org.sharetrace.util.IntervalCache;
import org.slf4j.Logger;

// TODO(rtatton) Add Javadoc
public class Node extends AbstractBehavior<NodeMessage> {

  private static final String CONTACT_PATTERN =
      "{\"event\": \"addContact\", \"nodes\": [\"{}\", \"{}\"]}";
  private static final String RECEIVE_PATTERN =
      "{\"event\": \"receive\", \"source\": \"{}\", \"target\": \"{}\", \"value\": {}, \"timestamp\": \"{}\", \"uuid\": \"{}\"}";
  private static final String BROADCAST_PATTERN =
      "{\"event\": \"broadcast\", \"source\": \"{}\", \"target\": \"{}\", \"value\": {}, \"timestamp\": \"{}\", \"uuid\": \"{}\"}";
  private static final String SEND_CURRENT_PATTERN =
      "{\"event\": \"sendCurrent\", \"source\": \"{}\", \"target\": \"{}\", \"value\": {}, \"timestamp\": \"{}\", \"uuid\": \"{}\"}";
  private static final String SEND_CACHED_PATTERN =
      "{\"event\": \"sendCached\", \"source\": \"{}\", \"target\": \"{}\", \"value\": {}, \"timestamp\": \"{}\", \"uuid\": \"{}\"}";
  private static final String UPDATE_PATTERN =
      "{\"event\": \"update\", \"source\": \"{}\", \"target\": \"{}\", \"previous\": {}, \"current\": {}, \"timestamp\": \"{}\", \"uuid\": \"{}\"}";

  private final Map<ActorRef<NodeMessage>, Instant> contacts;
  private final Parameters parameters;
  private final Supplier<Instant> clock;
  private final IntervalCache<RiskScore> cache;
  private RiskScore current;

  private Node(
      ActorContext<NodeMessage> context,
      Parameters parameters,
      Supplier<Instant> clock,
      IntervalCache<RiskScore> cache) {
    super(context);
    this.contacts = new HashMap<>();
    this.parameters = parameters;
    this.clock = clock;
    this.cache = cache;
    this.current = defaultScore();
    cache.put(current.timestamp(), current);
  }

  @Builder.Factory
  protected static Behavior<NodeMessage> node(
      Parameters parameters, Supplier<Instant> clock, IntervalCache<RiskScore> cache) {
    return Behaviors.setup(context -> new Node(context, parameters, clock, cache));
  }

  @Override
  public Receive<NodeMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(Contact.class, this::onContact)
        .onMessage(RiskScore.class, this::onRiskScore)
        .build();
  }

  private RiskScore defaultScore() {
    return RiskScore.builder()
        .replyTo(self())
        .value(RiskScore.MIN_VALUE)
        .timestamp(clock.get())
        .build();
  }

  private Behavior<NodeMessage> onContact(Contact contact) {
    logContact(contact);
    contacts.put(contact.replyTo(), contact.timestamp());
    if (!sendCurrent(contact)) {
      sendCached(contact);
    }
    return this;
  }

  private boolean sendCurrent(Contact contact) {
    boolean sent = isContactNewEnough(contact, current) && isScoreNewEnough(current);
    if (sent) {
      RiskScore transmitted = transmitted(current);
      logSendCurrent(contact.replyTo(), transmitted);
      contact.replyTo().tell(transmitted);
    }
    return sent;
  }

  private void sendCached(Contact contact) {
    RiskScore cached = cache.headMax(buffered(contact.timestamp()), RiskScore::compareTo);
    if (cached != null && isScoreNewEnough(cached)) {
      RiskScore transmitted = transmitted(cached);
      logSendCached(contact.replyTo(), transmitted);
      contact.replyTo().tell(transmitted);
    }
  }

  private Behavior<NodeMessage> onRiskScore(RiskScore score) {
    logReceive(score);
    update(score);
    broadcast(score);
    return this;
  }

  private void update(RiskScore score) {
    cache.put(score.timestamp(), score);
    if (score.value() > current.value()) {
      logUpdate(current, score);
      current = score;
    }
  }

  private void broadcast(RiskScore score) {
    RiskScore transmitted = transmitted(score);
    if (isScoreHighEnough(transmitted) && isScoreNewEnough(transmitted)) {
      contacts.entrySet().stream()
          .filter(isNotSender(score))
          .filter(isContactNewEnough(score))
          .forEach(contact -> sendBroadcast(contact.getKey(), transmitted));
    }
  }

  private RiskScore transmitted(RiskScore received) {
    return RiskScore.builder()
        .replyTo(self())
        .value(received.value() * parameters.transmissionRate())
        .timestamp(received.timestamp())
        .uuid(received.uuid())
        .build();
  }

  private boolean isScoreHighEnough(RiskScore score) {
    return score.value() >= current.value() * parameters.sendTolerance();
  }

  private Predicate<Entry<ActorRef<NodeMessage>, Instant>> isNotSender(RiskScore score) {
    return Predicate.not(contact -> Objects.equals(contact.getKey(), score.replyTo()));
  }

  private Predicate<Entry<ActorRef<NodeMessage>, Instant>> isContactNewEnough(RiskScore score) {
    return contact -> isTimeNewEnough(score, contact.getValue());
  }

  private boolean isContactNewEnough(Contact contact, RiskScore score) {
    return isTimeNewEnough(score, contact.timestamp());
  }

  private boolean isTimeNewEnough(RiskScore score, Instant timestamp) {
    return score.timestamp().isBefore(buffered(timestamp));
  }

  private Instant buffered(Instant timestamp) {
    return timestamp.plus(parameters.timeBuffer());
  }

  private boolean isScoreNewEnough(RiskScore score) {
    Duration sinceComputed = Duration.between(score.timestamp(), clock.get());
    return parameters.scoreTtl().compareTo(sinceComputed) > 0;
  }

  private void sendBroadcast(ActorRef<NodeMessage> contact, RiskScore score) {
    logBroadcast(contact, score);
    contact.tell(score);
  }

  private void logContact(Contact contact) {
    log().info(CONTACT_PATTERN, name(self()), name(contact.replyTo()));
  }

  private void logUpdate(RiskScore previous, RiskScore current) {
    log()
        .info(
            UPDATE_PATTERN,
            name(current.replyTo()),
            name(self()),
            previous.value(),
            current.value(),
            current.timestamp(),
            current.uuid());
  }

  private void logSendCurrent(ActorRef<NodeMessage> contact, RiskScore score) {
    logMessageOp(SEND_CURRENT_PATTERN, self(), contact, score);
  }

  private void logSendCached(ActorRef<NodeMessage> contact, RiskScore score) {
    logMessageOp(SEND_CACHED_PATTERN, self(), contact, score);
  }

  private void logBroadcast(ActorRef<NodeMessage> contact, RiskScore score) {
    logMessageOp(BROADCAST_PATTERN, self(), contact, score);
  }

  private void logReceive(RiskScore score) {
    logMessageOp(RECEIVE_PATTERN, score.replyTo(), self(), score);
  }

  private void logMessageOp(
      String pattern, ActorRef<NodeMessage> source, ActorRef<NodeMessage> target, RiskScore score) {
    log().info(pattern, name(source), name(target), score.value(), score.timestamp(), score.uuid());
  }

  private Logger log() {
    return getContext().getLog();
  }

  private String name(ActorRef<NodeMessage> node) {
    return node.path().name();
  }

  private ActorRef<NodeMessage> self() {
    return getContext().getSelf();
  }
}

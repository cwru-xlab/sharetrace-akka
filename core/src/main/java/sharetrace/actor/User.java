package sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import java.time.Clock;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import org.immutables.builder.Builder;
import sharetrace.model.Parameters;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.model.message.AlgorithmMessage;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.TimedOutMessage;
import sharetrace.model.message.UserMessage;
import sharetrace.util.RangeCache;
import sharetrace.util.RangeCacheBuilder;
import sharetrace.util.StandardCache;
import sharetrace.util.StandardCacheBuilder;
import sharetrace.util.logging.Logging;
import sharetrace.util.logging.RecordLogger;
import sharetrace.util.logging.event.ContactEvent;
import sharetrace.util.logging.event.EventRecord;
import sharetrace.util.logging.event.ReceiveEvent;
import sharetrace.util.logging.event.SendEvent;
import sharetrace.util.logging.event.UpdateEvent;

final class User extends AbstractBehavior<UserMessage> {

  private static final RecordLogger<EventRecord> LOGGER = Logging.eventsLogger();

  private final ActorRef<AlgorithmMessage> monitor;
  private final TimedOutMessage timedOutMessage;
  private final TimerScheduler<UserMessage> timers;
  private final Parameters parameters;
  private final Clock clock;
  private final RangeCache<RiskScoreMessage> scores;
  private final StandardCache<ActorRef<?>, Contact> contacts;

  private RiskScoreMessage exposureScore;

  private User(
      ActorContext<UserMessage> context,
      ActorRef<AlgorithmMessage> monitor,
      TimedOutMessage timedOutMessage,
      TimerScheduler<UserMessage> timers,
      Parameters parameters,
      Clock clock) {
    super(context);
    this.monitor = monitor;
    this.timers = timers;
    this.parameters = parameters;
    this.clock = clock;
    this.timedOutMessage = timedOutMessage;
    this.scores = newScoreCache();
    this.contacts = newContactCache();
    this.exposureScore = defaultExposureScore();
  }

  private RangeCache<RiskScoreMessage> newScoreCache() {
    return RangeCacheBuilder.<RiskScoreMessage>create()
        .clock(clock)
        .comparator(TemporalScore::compareTo)
        .merger(BinaryOperator.maxBy(TemporalScore::compareTo))
        .build();
  }

  private StandardCache<ActorRef<?>, Contact> newContactCache() {
    return StandardCacheBuilder.<ActorRef<?>, Contact>create()
        .clock(clock)
        .comparator(Contact::compareTo)
        .merger(BinaryOperator.maxBy(Contact::compareTo))
        .build();
  }

  @Builder.Factory
  static Behavior<UserMessage> user(
      ActorRef<AlgorithmMessage> monitor,
      TimedOutMessage timedOutMessage,
      Parameters parameters,
      Clock clock) {
    return Behaviors.setup(
        context -> {
          Behavior<UserMessage> user =
              Behaviors.withTimers(
                  timers -> new User(context, monitor, timedOutMessage, timers, parameters, clock));
          return Behaviors.withMdc(UserMessage.class, Logging.getMdc(), user);
        });
  }

  @Override
  public Receive<UserMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(ContactMessage.class, this::handle)
        .onMessage(RiskScoreMessage.class, this::handle)
        .onMessage(TimedOutMessage.class, this::handle)
        .build();
  }

  private Behavior<UserMessage> handle(ContactMessage message) {
    if (message.isAlive(clock)) {
      Contact contact = newContact(message);
      contacts.put(contact.self(), contact);
      logContactEvent(contact);
      sendCachedMessage(contact);
    }
    return this;
  }

  private Contact newContact(ContactMessage message) {
    return ContactBuilder.create()
        .message(message)
        .parameters(parameters)
        .scores(scores)
        .clock(clock)
        .build();
  }

  private void sendCachedMessage(Contact contact) {
    scores
        .refresh()
        .max(contact.bufferedTimestamp())
        .ifPresent(message -> contact.tell(message, this::logSendEvent));
  }

  private Behavior<UserMessage> handle(RiskScoreMessage message) {
    logReceiveEvent(message);
    if (message.isAlive(clock)) {
      RiskScoreMessage transmitted = transmitted(message);
      scores.add(transmitted);
      updateExposureScore(message);
      contacts.refresh().forEach(contact -> contact.tell(transmitted));
    }
    timers.startSingleTimer(timedOutMessage, parameters.idleTimeout());
    return this;
  }

  private RiskScoreMessage transmitted(RiskScoreMessage message) {
    return message.withSender(getContext().getSelf()).mapScore(this::transmitted);
  }

  private RiskScore transmitted(RiskScore score) {
    return score.mapValue(value -> value * parameters.transmissionRate());
  }

  private RiskScoreMessage preTransmission(RiskScoreMessage message) {
    return message.mapScore(this::preTransmission);
  }

  private RiskScore preTransmission(RiskScore score) {
    return score.mapValue(value -> value / parameters.transmissionRate());
  }

  private void updateExposureScore(RiskScoreMessage message) {
    RiskScoreMessage previous = exposureScore;
    if (exposureScore.value() < message.value()) {
      exposureScore = message;
      logUpdateEvent(previous, exposureScore);
    } else if (exposureScore.isExpired(clock)) {
      scores.refresh();
      exposureScore = scores.max().map(this::preTransmission).orElseGet(this::defaultExposureScore);
      logUpdateEvent(previous, exposureScore);
    }
  }

  private Behavior<UserMessage> handle(TimedOutMessage message) {
    monitor.tell(message);
    return this;
  }

  private RiskScoreMessage defaultExposureScore() {
    return RiskScoreMessage.builder().score(RiskScore.MIN).sender(getContext().getSelf()).build();
  }

  private void logContactEvent(Contact contact) {
    logEvent(ContactEvent.class, () -> contactEvent(contact));
  }

  private ContactEvent contactEvent(Contact contact) {
    return ContactEvent.builder()
        .self(getContext().getSelf())
        .contact(contact.self())
        .timestamp(clock.instant())
        .build();
  }

  private void logSendEvent(ActorRef<?> contact, RiskScoreMessage message) {
    logEvent(SendEvent.class, () -> sendEvent(contact, message));
  }

  private SendEvent sendEvent(ActorRef<?> contact, RiskScoreMessage message) {
    return SendEvent.builder()
        .self(getContext().getSelf())
        .message(message)
        .contact(contact)
        .timestamp(clock.instant())
        .build();
  }

  private void logReceiveEvent(RiskScoreMessage message) {
    logEvent(ReceiveEvent.class, () -> receiveEvent(message));
  }

  private ReceiveEvent receiveEvent(RiskScoreMessage message) {
    return ReceiveEvent.builder()
        .self(getContext().getSelf())
        .contact(message.sender())
        .message(message)
        .timestamp(clock.instant())
        .build();
  }

  private void logUpdateEvent(RiskScoreMessage previous, RiskScoreMessage current) {
    logEvent(UpdateEvent.class, () -> updateEvent(previous, current));
  }

  private UpdateEvent updateEvent(RiskScoreMessage previous, RiskScoreMessage current) {
    return UpdateEvent.builder()
        .self(getContext().getSelf())
        .previous(previous)
        .current(current)
        .timestamp(clock.instant())
        .build();
  }

  private <T extends EventRecord> void logEvent(Class<T> type, Supplier<T> event) {
    LOGGER.log(EventRecord.KEY, type, event);
  }
}

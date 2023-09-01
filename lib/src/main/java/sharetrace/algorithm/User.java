package sharetrace.algorithm;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.PostStop;
import akka.actor.typed.Props;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import java.time.Instant;
import java.time.InstantSource;
import java.util.function.Function;
import sharetrace.cache.Cache;
import sharetrace.cache.RangeCache;
import sharetrace.cache.StandardCache;
import sharetrace.logging.RecordLogger;
import sharetrace.logging.event.ContactEvent;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.LastEvent;
import sharetrace.logging.event.ReceiveEvent;
import sharetrace.logging.event.SendEvent;
import sharetrace.logging.event.UpdateEvent;
import sharetrace.model.Parameters;
import sharetrace.model.RiskScore;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.IdleTimeout;
import sharetrace.model.message.MonitorMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.UserMessage;
import sharetrace.util.Context;

final class User extends AbstractBehavior<UserMessage> {

  private final RecordLogger<Event> logger;
  private final InstantSource timeSource;
  private final int id;
  private final Parameters parameters;
  private final ActorRef<MonitorMessage> monitor;
  private final IdleTimeout idleTimeout;
  private final TimerScheduler<UserMessage> timers;
  private final Cache<RiskScoreMessage> scores;
  private final Cache<Contact> contacts;
  private RiskScoreMessage exposureScore;
  private Instant lastEvent;

  private User(
      ActorContext<UserMessage> ctx,
      Context context,
      Parameters parameters,
      int id,
      ActorRef<MonitorMessage> monitor,
      IdleTimeout idleTimeout,
      TimerScheduler<UserMessage> timers) {
    super(ctx);
    this.logger = context.eventsLogger();
    this.timeSource = context.timeSource();
    this.parameters = parameters;
    this.id = id;
    this.monitor = monitor;
    this.idleTimeout = idleTimeout;
    this.timers = timers;
    this.scores = new RangeCache<>(timeSource);
    this.contacts = new StandardCache<>(timeSource);
    this.exposureScore = defaultExposureScore();
    this.lastEvent = Instant.EPOCH;
  }

  public static Behavior<UserMessage> of(
      Context context,
      Parameters parameters,
      int id,
      ActorRef<MonitorMessage> monitor,
      IdleTimeout idleTimeout) {
    return Behaviors.setup(
        ctx -> {
          var user =
              Behaviors.<UserMessage>withTimers(
                  timers -> new User(ctx, context, parameters, id, monitor, idleTimeout, timers));
          return Behaviors.withMdc(UserMessage.class, context.mdc(), user);
        });
  }

  public static Props props() {
    return DispatcherSelector.fromConfig("sharetrace.user.dispatcher");
  }

  @Override
  public Receive<UserMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(ContactMessage.class, this::handle)
        .onMessage(RiskScoreMessage.class, this::handle)
        .onMessage(IdleTimeout.class, this::handle)
        .onSignal(PostStop.class, this::handle)
        .build();
  }

  private Behavior<UserMessage> handle(ContactMessage message) {
    var contact = new Contact(message, parameters, scores, timeSource);
    if (contact.isAlive(timeSource)) {
      contacts.add(contact);
      logContactEvent(contact);
    }
    /* Always try to send a new contact a risk score. An expired contact may still receive a risk
    score if it is "relevant" (i.e., within the time buffer of the contact time). */
    scores
        .refresh()
        .max(contact.bufferedTimestamp())
        .ifPresent(msg -> contact.tell(msg, this::logSendEvent));
    return this;
  }

  private Behavior<UserMessage> handle(RiskScoreMessage message) {
    logReceiveEvent(message);
    if (message.isAlive(timeSource)) {
      var transmitted = transmitted(message);
      scores.add(transmitted);
      updateExposureScore(message);
      contacts.refresh().forEach(contact -> contact.tell(transmitted));
    }
    timers.startSingleTimer(idleTimeout, parameters.idleTimeout());
    return this;
  }

  private void updateExposureScore(RiskScoreMessage message) {
    var previous = exposureScore;
    if (exposureScore.value() < message.value()) {
      exposureScore = message;
      logUpdateEvent(previous, exposureScore);
    } else if (exposureScore.isExpired(timeSource)) {
      scores.refresh();
      exposureScore = scores.max().map(this::preTransmission).orElseGet(this::defaultExposureScore);
      logUpdateEvent(previous, exposureScore);
    }
  }

  private Behavior<UserMessage> handle(IdleTimeout timeout) {
    monitor.tell(timeout);
    return this;
  }

  private Behavior<UserMessage> handle(PostStop stop) {
    logLastEvent();
    return this;
  }

  private RiskScoreMessage transmitted(RiskScoreMessage message) {
    var score = message.score().mapValue(value -> value * parameters.transmissionRate());
    return new RiskScoreMessage(self(), score, message.id());
  }

  private RiskScoreMessage preTransmission(RiskScoreMessage message) {
    var score = message.score().mapValue(value -> value / parameters.transmissionRate());
    return new RiskScoreMessage(message.sender(), score, message.id());
  }

  private RiskScoreMessage defaultExposureScore() {
    return new RiskScoreMessage(self(), RiskScore.MIN, id);
  }

  private void logContactEvent(Contact contact) {
    var contactTime = contact.timestamp();
    logEvent(ContactEvent.class, t -> new ContactEvent(self(), contact.self(), contactTime, t));
  }

  private void logSendEvent(ActorRef<?> contact, RiskScoreMessage message) {
    logEvent(SendEvent.class, t -> new SendEvent(self(), contact, message, t));
  }

  private void logReceiveEvent(RiskScoreMessage message) {
    logEvent(ReceiveEvent.class, t -> new ReceiveEvent(self(), message, t));
  }

  private void logUpdateEvent(RiskScoreMessage previous, RiskScoreMessage current) {
    logEvent(UpdateEvent.class, t -> new UpdateEvent(self(), previous, current, t));
  }

  private void logLastEvent() {
    logger.log(LastEvent.class, () -> new LastEvent(self(), lastEvent));
  }

  private <T extends Event> void logEvent(Class<T> type, Function<Instant, T> factory) {
    lastEvent = timeSource.instant();
    logger.log(type, () -> factory.apply(lastEvent));
  }

  private ActorRef<UserMessage> self() {
    return getContext().getSelf();
  }
}

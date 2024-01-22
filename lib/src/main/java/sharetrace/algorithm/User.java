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
import java.time.Duration;
import java.util.function.LongFunction;
import sharetrace.logging.RecordLogger;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.user.ContactEvent;
import sharetrace.logging.event.user.LastEvent;
import sharetrace.logging.event.user.ReceiveEvent;
import sharetrace.logging.event.user.SendEvent;
import sharetrace.logging.event.user.UpdateEvent;
import sharetrace.model.Parameters;
import sharetrace.model.RiskScore;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.MonitorMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.TimeoutMessage;
import sharetrace.model.message.UserMessage;
import sharetrace.util.Cache;
import sharetrace.util.Context;

final class User extends AbstractBehavior<UserMessage> {

  private final int id;
  private final Context context;
  private final Parameters parameters;
  private final ActorRef<MonitorMessage> monitor;
  private final TimerScheduler<UserMessage> timers;
  private final Duration timeout;
  private final TimeoutMessage timeoutMessage;
  private final Cache<RiskScoreMessage> scores;
  private final Cache<Contact> contacts;

  private RiskScoreMessage currentScore;
  private long lastEventTime;

  private User(
      int id,
      ActorContext<UserMessage> actorContext,
      Context context,
      Parameters parameters,
      ActorRef<MonitorMessage> monitor,
      TimerScheduler<UserMessage> timers) {
    super(actorContext);
    this.id = id;
    this.context = context;
    this.parameters = parameters;
    this.monitor = monitor;
    this.timers = timers;
    this.timeoutMessage = new TimeoutMessage(id);
    this.timeout = Duration.ofMillis(parameters.timeout());
    this.scores = new TemopralScoreCache<>(context.timeSource());
    this.contacts = new ContactCache(context.timeSource());
    this.currentScore = defaultScore();
  }

  public static Behavior<UserMessage> of(
      int id, Context context, Parameters parameters, ActorRef<MonitorMessage> monitor) {
    return Behaviors.setup(
        ctx -> {
          var user =
              Behaviors.<UserMessage>withTimers(
                  timers -> new User(id, ctx, context, parameters, monitor, timers));
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
        .onMessage(TimeoutMessage.class, this::handle)
        .onSignal(PostStop.class, this::handle)
        .build();
  }

  private Behavior<UserMessage> handle(ContactMessage message) {
    var contact = new Contact(message, parameters, scores, context.timeSource());
    if (!contact.isExpired(currentTime())) {
      contacts.add(contact);
      logContactEvent(contact);
    }
    contact.tellInitialMessage(this::logSendEvent);
    return this;
  }

  private Behavior<UserMessage> handle(RiskScoreMessage message) {
    logReceiveEvent(message);
    if (!message.isExpired(currentTime())) {
      var transmitted = transmitted(message);
      scores.add(transmitted);
      updateExposureScore(message);
      contacts.refresh().forEach(contact -> contact.tell(transmitted));
    }
    timers.startSingleTimer(timeoutMessage, timeout);
    return this;
  }

  private void updateExposureScore(RiskScoreMessage message) {
    if (currentScore.value() < message.value()) {
      var previousScore = currentScore;
      currentScore = message;
      logUpdateEvent(previousScore, currentScore);
    } else if (currentScore.isExpired(currentTime())) {
      var previousScore = currentScore;
      currentScore = scores.refresh().max().map(this::untransmitted).orElseGet(this::defaultScore);
      logUpdateEvent(previousScore, currentScore);
    }
  }

  private Behavior<UserMessage> handle(TimeoutMessage message) {
    monitor.tell(message);
    return this;
  }

  private Behavior<UserMessage> handle(PostStop stop) {
    logLastEvent();
    return this;
  }

  private RiskScoreMessage transmitted(RiskScoreMessage message) {
    var score = message.score().mapValue(value -> value * parameters.transmissionRate());
    return new RiskScoreMessage(score, id, message.origin());
  }

  @SuppressWarnings("SpellCheckingInspection")
  private RiskScoreMessage untransmitted(RiskScoreMessage message) {
    var score = message.score().mapValue(value -> value / parameters.transmissionRate());
    return new RiskScoreMessage(score, message.sender(), message.origin());
  }

  private RiskScoreMessage defaultScore() {
    return new RiskScoreMessage(RiskScore.MIN, id, id);
  }

  private void logContactEvent(Contact contact) {
    var contactTime = contact.timestamp();
    logEvent(ContactEvent.class, t -> new ContactEvent(id, contact.id(), contactTime, t));
  }

  private void logSendEvent(int contactId, RiskScoreMessage message) {
    logEvent(SendEvent.class, t -> new SendEvent(id, contactId, message, t));
  }

  private void logReceiveEvent(RiskScoreMessage message) {
    logEvent(ReceiveEvent.class, t -> new ReceiveEvent(id, message, t));
  }

  private void logUpdateEvent(RiskScoreMessage previous, RiskScoreMessage current) {
    logEvent(UpdateEvent.class, t -> new UpdateEvent(id, previous, current, t));
  }

  private void logLastEvent() {
    logger().log(LastEvent.class, () -> new LastEvent(id, lastEventTime));
  }

  private <T extends Event> void logEvent(Class<T> type, LongFunction<T> factory) {
    lastEventTime = currentTime();
    logger().log(type, () -> factory.apply(lastEventTime));
  }

  private RecordLogger logger() {
    return context.eventLogger();
  }

  private long currentTime() {
    return context.timeSource().millis();
  }
}

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
import java.util.function.LongFunction;
import sharetrace.logging.RecordLogger;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.user.ContactEvent;
import sharetrace.logging.event.user.LastEvent;
import sharetrace.logging.event.user.ReceiveEvent;
import sharetrace.logging.event.user.UpdateEvent;
import sharetrace.model.Parameters;
import sharetrace.model.RiskScore;
import sharetrace.model.message.BatchTimeoutMessage;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.IdleTimeoutMessage;
import sharetrace.model.message.MonitorMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.UserMessage;
import sharetrace.util.Cache;
import sharetrace.util.Context;

final class User extends AbstractBehavior<UserMessage> {

  private final int id;
  private final Context context;
  private final Parameters parameters;
  private final ActorRef<MonitorMessage> monitor;
  private final TimerScheduler<UserMessage> timers;
  private final IdleTimeoutMessage idleTimeoutMessage;
  private final Cache<RiskScoreMessage> scores;
  private final Cache<Contact> contacts;
  private final RiskScoreMessage defaultScore;

  private RiskScoreMessage exposureScore;
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
    this.idleTimeoutMessage = new IdleTimeoutMessage(id);
    this.scores = new TemopralScoreCache<>(context.timeSource());
    this.contacts = new ContactCache(context.timeSource());
    this.defaultScore = RiskScoreMessage.ofOrigin(RiskScore.MIN, id);
    this.exposureScore = defaultScore;
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
        .onMessage(BatchTimeoutMessage.class, this::handle)
        .onMessage(IdleTimeoutMessage.class, this::handle)
        .onSignal(PostStop.class, this::handle)
        .build();
  }

  private Behavior<UserMessage> handle(ContactMessage message) {
    if (!message.isExpired(currentTime())) {
      var contact = new Contact(message, parameters, context.timeSource());
      contacts.add(contact);
      contact.applyCached(scores);
      logContactEvent(contact);
    }
    return this;
  }

  private Behavior<UserMessage> handle(RiskScoreMessage message) {
    logReceiveEvent(message);
    if (!message.isExpired(currentTime())) {
      var transmitted = transmitted(message);
      scores.add(transmitted);
      updateExposureScore(message);
      contacts.forEach(contact -> contact.apply(transmitted, scores));
    }
    startTimers();
    return this;
  }

  private void updateExposureScore(RiskScoreMessage message) {
    if (exposureScore.value() < message.value()) {
      var previous = exposureScore;
      exposureScore = message;
      logUpdateEvent(previous, exposureScore);
    } else if (exposureScore.isExpired(currentTime())) {
      var previous = exposureScore;
      exposureScore = scores.refresh().max().map(this::untransmitted).orElse(this.defaultScore);
      logUpdateEvent(previous, exposureScore);
    }
  }

  private void startTimers() {
    if (!timers.isTimerActive(BatchTimeoutMessage.INSTANCE)) {
      timers.startTimerWithFixedDelay(BatchTimeoutMessage.INSTANCE, parameters.batchTimeout());
    }
    timers.startSingleTimer(idleTimeoutMessage, parameters.idleTimeout());
  }

  private Behavior<UserMessage> handle(BatchTimeoutMessage message) {
    contacts.forEach(Contact::flush);
    contacts.refresh();
    return this;
  }

  private Behavior<UserMessage> handle(IdleTimeoutMessage message) {
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

  private void logContactEvent(Contact contact) {
    logEvent(ContactEvent.class, t -> new ContactEvent(id, contact.id(), contact.timestamp(), t));
  }

  private void logReceiveEvent(RiskScoreMessage message) {
    logEvent(ReceiveEvent.class, t -> new ReceiveEvent(id, message.sender(), message, t));
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

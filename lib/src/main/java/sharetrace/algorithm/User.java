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
import com.google.common.collect.Range;
import java.util.function.LongFunction;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.user.ContactEvent;
import sharetrace.logging.event.user.LastEvent;
import sharetrace.logging.event.user.ReceiveEvent;
import sharetrace.logging.event.user.UpdateEvent;
import sharetrace.model.Context;
import sharetrace.model.Expirable;
import sharetrace.model.Parameters;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.FlushTimeoutMessage;
import sharetrace.model.message.MonitorMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.UserMessage;
import sharetrace.model.message.UserUpdatedMessage;

final class User extends AbstractBehavior<UserMessage> {

  private final int id;
  private final Context context;
  private final Parameters parameters;
  private final ActorRef<MonitorMessage> monitor;
  private final TimerScheduler<UserMessage> timers;
  private final RiskScoreMessageStore scores;
  private final ContactStore contacts;

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
    this.scores = new RiskScoreMessageStore(context);
    this.contacts = new ContactStore(context);
    this.exposureScore = RiskScoreMessage.NULL;
  }

  public static Behavior<UserMessage> of(
      int id, Context context, Parameters parameters, ActorRef<MonitorMessage> monitor) {
    return Behaviors.setup(
        actorContext -> {
          var user =
              Behaviors.<UserMessage>withTimers(
                  timers -> new User(id, actorContext, context, parameters, monitor, timers));
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
        .onMessage(FlushTimeoutMessage.class, this::handle)
        .onSignal(PostStop.class, this::handle)
        .build();
  }

  private Behavior<UserMessage> handle(ContactMessage message) {
    if (!isExpired(message)) {
      var contact = new Contact(message, parameters, context);
      contacts.add(contact);
      contact.apply(scores);
      logContactEvent(contact);
    }
    return this;
  }

  private Behavior<UserMessage> handle(RiskScoreMessage message) {
    logReceiveEvent(message);
    if (!isExpired(message)) {
      updateExposureScore(message);
      var transmitted = transmitted(message);
      scores.add(transmitted);
      contacts.forEach(contact -> contact.apply(transmitted, scores));
    }
    startFlushTimeoutTimer();
    return this;
  }

  private void updateExposureScore(RiskScoreMessage message) {
    if (exposureScore.value() < message.value()) {
      var previous = exposureScore;
      exposureScore = message;
      onUpdate(previous);
    } else if (isExpired(exposureScore)) {
      var previous = exposureScore;
      exposureScore = scores.max(Range.all()).map(this::original).orElse(RiskScoreMessage.NULL);
      onUpdate(previous);
    }
  }

  private void onUpdate(RiskScoreMessage previous) {
    logUpdateEvent(previous, exposureScore);
    monitor.tell(UserUpdatedMessage.INSTANCE);
  }

  private void startFlushTimeoutTimer() {
    if (!timers.isTimerActive(FlushTimeoutMessage.INSTANCE)) {
      timers.startTimerWithFixedDelay(FlushTimeoutMessage.INSTANCE, parameters.flushTimeout());
    }
  }

  @SuppressWarnings("unused")
  private Behavior<UserMessage> handle(FlushTimeoutMessage message) {
    contacts.forEach(Contact::flush);
    contacts.refresh();
    return this;
  }

  @SuppressWarnings("unused")
  private Behavior<UserMessage> handle(PostStop stop) {
    logLastEvent();
    return this;
  }

  private boolean isExpired(Expirable expirable) {
    return expirable.isExpired(context);
  }

  private RiskScoreMessage transmitted(RiskScoreMessage message) {
    var score = message.score().mapValue(value -> value * parameters.transmissionRate());
    return new RiskScoreMessage(score, id, message.origin());
  }

  private RiskScoreMessage original(RiskScoreMessage message) {
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
    context.logEvent(LastEvent.class, () -> new LastEvent(id, lastEventTime));
  }

  private <T extends Event> void logEvent(Class<T> type, LongFunction<T> factory) {
    lastEventTime = context.getTime();
    context.logEvent(type, () -> factory.apply(lastEventTime));
  }
}

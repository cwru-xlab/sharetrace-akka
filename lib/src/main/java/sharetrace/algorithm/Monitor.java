package sharetrace.algorithm;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import org.slf4j.MDC;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.lifecycle.CreateUsersEnd;
import sharetrace.logging.event.lifecycle.CreateUsersStart;
import sharetrace.logging.event.lifecycle.RiskPropagationEnd;
import sharetrace.logging.event.lifecycle.RiskPropagationStart;
import sharetrace.logging.event.lifecycle.SendContactsEnd;
import sharetrace.logging.event.lifecycle.SendContactsStart;
import sharetrace.logging.event.lifecycle.SendRiskScoresEnd;
import sharetrace.logging.event.lifecycle.SendRiskScoresStart;
import sharetrace.model.Context;
import sharetrace.model.Parameters;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.model.graph.ContactNetwork;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.IdleTimeoutMessage;
import sharetrace.model.message.MonitorMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.RunMessage;
import sharetrace.model.message.UserMessage;
import sharetrace.model.message.UserUpdatedMessage;

final class Monitor extends AbstractBehavior<MonitorMessage> {

  private final Context context;
  private final Parameters parameters;
  private final RiskScoreFactory scoreFactory;
  private final ContactNetwork network;
  private final TimerScheduler<MonitorMessage> timers;

  private Monitor(
      ActorContext<MonitorMessage> actorContext,
      Context context,
      Parameters parameters,
      RiskScoreFactory scoreFactory,
      ContactNetwork network,
      TimerScheduler<MonitorMessage> timers) {
    super(actorContext);
    this.context = context;
    this.parameters = parameters;
    this.scoreFactory = scoreFactory;
    this.network = network;
    this.timers = timers;
  }

  public static Behavior<MonitorMessage> of(
      Context context,
      Parameters parameters,
      RiskScoreFactory scoreFactory,
      ContactNetwork network) {
    return Behaviors.setup(
        actorContext -> {
          var monitor =
              Behaviors.<MonitorMessage>withTimers(
                  timers ->
                      new Monitor(
                          actorContext, context, parameters, scoreFactory, network, timers));
          return Behaviors.withMdc(MonitorMessage.class, context.mdc(), monitor);
        });
  }

  @Override
  public Receive<MonitorMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(RunMessage.class, this::handle)
        .onMessage(UserUpdatedMessage.class, this::handle)
        .onMessage(IdleTimeoutMessage.class, this::handle)
        .onSignal(PostStop.class, this::handle)
        .build();
  }

  @SuppressWarnings("unused")
  private Behavior<MonitorMessage> handle(RunMessage message) {
    logEvent(new RiskPropagationStart());
    var users = createUsers();
    sendContacts(users);
    sendRiskScores(users);
    startIdleTimeoutTimer();
    return this;
  }

  @SuppressWarnings("unchecked")
  private ActorRef<UserMessage>[] createUsers() {
    logEvent(new CreateUsersStart());
    var users = new ActorRef[network.vertexSet().size()];
    var props = DispatcherSelector.fromConfig("sharetrace.user.dispatcher");
    for (int i : network.vertexSet()) {
      var behavior = User.of(i, context, parameters, getContext().getSelf());
      users[i] = getContext().spawn(behavior, "User-" + i, props);
      getContext().watch(users[i]);
    }
    logEvent(new CreateUsersEnd());
    return users;
  }

  private void sendContacts(ActorRef<UserMessage>[] users) {
    logEvent(new SendContactsStart());
    var expiry = parameters.contactExpiry();
    for (var edge : network.edgeSet()) {
      int i = network.getEdgeSource(edge);
      int j = network.getEdgeTarget(edge);
      users[i].tell(ContactMessage.fromExpiry(users[j], j, edge.getTime(), expiry));
      users[j].tell(ContactMessage.fromExpiry(users[i], i, edge.getTime(), expiry));
    }
    logEvent(new SendContactsEnd());
  }

  private void sendRiskScores(ActorRef<UserMessage>[] users) {
    logEvent(new SendRiskScoresStart());
    for (int i : network.vertexSet()) {
      var score = scoreFactory.getRiskScore(i);
      users[i].tell(RiskScoreMessage.ofOrigin(score, i));
    }
    logEvent(new SendRiskScoresEnd());
  }

  @SuppressWarnings("unused")
  private Behavior<MonitorMessage> handle(UserUpdatedMessage message) {
    startIdleTimeoutTimer();
    return this;
  }

  private void startIdleTimeoutTimer() {
    timers.startSingleTimer(IdleTimeoutMessage.INSTANCE, parameters.idleTimeout());
  }

  @SuppressWarnings("unused")
  private Behavior<MonitorMessage> handle(IdleTimeoutMessage message) {
    return Behaviors.stopped();
  }

  @SuppressWarnings("unused")
  private Behavior<MonitorMessage> handle(PostStop stop) {
    // Logging this in response to a PostStop signal is the only way that works.
    // Set the MDC since Akka sometimes clears it before this event is logged.
    MDC.setContextMap(context.mdc());
    logEvent(new RiskPropagationEnd());
    return this;
  }

  private void logEvent(Event event) {
    context.eventLogger().log(event);
  }
}

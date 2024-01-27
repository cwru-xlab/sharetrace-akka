package sharetrace.algorithm;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.Props;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import java.util.BitSet;
import java.util.function.LongFunction;
import sharetrace.graph.ContactNetwork;
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
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.IdleTimeoutMessage;
import sharetrace.model.message.MonitorMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.RunMessage;
import sharetrace.model.message.UserMessage;

final class Monitor extends AbstractBehavior<MonitorMessage> {

  private final Context context;
  private final Parameters parameters;
  private final RiskScoreFactory scoreFactory;
  private final ContactNetwork contactNetwork;
  private final int userCount;
  private final BitSet timeouts;

  private Monitor(
      ActorContext<MonitorMessage> ctx,
      Context context,
      Parameters parameters,
      RiskScoreFactory scoreFactory,
      ContactNetwork contactNetwork) {
    super(ctx);
    this.context = context;
    this.parameters = parameters;
    this.scoreFactory = scoreFactory;
    this.contactNetwork = contactNetwork;
    this.userCount = contactNetwork.vertexSet().size();
    this.timeouts = new BitSet(userCount);
  }

  public static Behavior<MonitorMessage> of(
      Context context,
      Parameters parameters,
      RiskScoreFactory scoreFactory,
      ContactNetwork contactNetwork) {
    return Behaviors.setup(
        ctx -> {
          var monitor = new Monitor(ctx, context, parameters, scoreFactory, contactNetwork);
          return Behaviors.withMdc(MonitorMessage.class, context.mdc(), monitor);
        });
  }

  public static Props props() {
    return DispatcherSelector.fromConfig("sharetrace.monitor.dispatcher");
  }

  public static String name() {
    return Monitor.class.getSimpleName();
  }

  @Override
  public Receive<MonitorMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(RunMessage.class, this::handle)
        .onMessage(IdleTimeoutMessage.class, this::handle)
        .build();
  }

  private Behavior<MonitorMessage> handle(RunMessage message) {
    if (userCount < 1) {
      return Behaviors.stopped();
    }
    logEvent(RiskPropagationStart.class, RiskPropagationStart::new);
    var users = newUsers();
    sendContacts(users);
    sendRiskScores(users);
    return this;
  }

  private Behavior<MonitorMessage> handle(IdleTimeoutMessage message) {
    timeouts.set(message.id());
    if (timeouts.cardinality() < userCount) {
      return this;
    }
    logEvent(RiskPropagationEnd.class, RiskPropagationEnd::new);
    return Behaviors.stopped();
  }

  @SuppressWarnings("unchecked")
  private ActorRef<UserMessage>[] newUsers() {
    logEvent(CreateUsersStart.class, CreateUsersStart::new);
    var users = new ActorRef[userCount];
    for (int i : contactNetwork.vertexSet()) {
      var user = User.of(i, context, parameters, getContext().getSelf());
      users[i] = getContext().spawnAnonymous(user, User.props());
      getContext().watch(users[i]);
    }
    logEvent(CreateUsersEnd.class, CreateUsersEnd::new);
    return users;
  }

  private void sendContacts(ActorRef<UserMessage>[] users) {
    logEvent(SendContactsStart.class, SendContactsStart::new);
    var expiry = parameters.contactExpiry();
    for (var edge : contactNetwork.edgeSet()) {
      int i = contactNetwork.getEdgeSource(edge);
      int j = contactNetwork.getEdgeTarget(edge);
      users[i].tell(ContactMessage.fromExpiry(users[j], j, edge.getTime(), expiry));
      users[j].tell(ContactMessage.fromExpiry(users[i], i, edge.getTime(), expiry));
    }
    logEvent(SendContactsEnd.class, SendContactsEnd::new);
  }

  private void sendRiskScores(ActorRef<UserMessage>[] users) {
    logEvent(SendRiskScoresStart.class, SendRiskScoresStart::new);
    for (var i = 0; i < userCount; i++) {
      var score = scoreFactory.getRiskScore(i);
      users[i].tell(RiskScoreMessage.ofOrigin(score, i));
    }
    logEvent(SendRiskScoresEnd.class, SendRiskScoresEnd::new);
  }

  private <T extends Event> void logEvent(Class<T> type, LongFunction<T> factory) {
    context.eventLogger().log(type, () -> factory.apply(context.timeSource().millis()));
  }
}

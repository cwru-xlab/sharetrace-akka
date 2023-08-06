package sharetrace.algorithm;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.Props;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.time.Instant;
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;
import sharetrace.graph.ContactNetwork;
import sharetrace.logging.RecordLogger;
import sharetrace.logging.event.CreateUsersEnd;
import sharetrace.logging.event.CreateUsersStart;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.RiskPropagationEnd;
import sharetrace.logging.event.RiskPropagationStart;
import sharetrace.logging.event.SendContactsEnd;
import sharetrace.logging.event.SendContactsStart;
import sharetrace.logging.event.SendRiskScoresEnd;
import sharetrace.logging.event.SendRiskScoresStart;
import sharetrace.model.Parameters;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.IdleTimeout;
import sharetrace.model.message.MonitorMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.Run;
import sharetrace.model.message.UserMessage;
import sharetrace.util.Context;

final class Monitor extends AbstractBehavior<MonitorMessage> {

  private final Context context;
  private final RecordLogger<Event> logger;
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
    this.logger = context.eventsLogger();
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
        .onMessage(Run.class, this::handle)
        .onMessage(IdleTimeout.class, this::handle)
        .build();
  }

  private Behavior<MonitorMessage> handle(Run run) {
    if (userCount < 1) {
      return Behaviors.stopped();
    }
    logEvent(RiskPropagationStart.class, RiskPropagationStart::new);
    var users = newUsers();
    sendContacts(users);
    sendRiskScores(users);
    return this;
  }

  private Behavior<MonitorMessage> handle(IdleTimeout timeout) {
    timeouts.set(timeout.key());
    if (timeouts.cardinality() == userCount) {
      logEvent(RiskPropagationEnd.class, RiskPropagationEnd::new);
      return Behaviors.stopped();
    }
    return this;
  }

  private List<ActorRef<UserMessage>> newUsers() {
    logEvent(CreateUsersStart.class, CreateUsersStart::new);
    var users = new ObjectArrayList<ActorRef<UserMessage>>(userCount);
    for (var key : contactNetwork.vertexSet()) {
      var user = User.of(context, parameters, getContext().getSelf(), new IdleTimeout(key));
      var reference = getContext().spawn(user, String.valueOf(key), User.props());
      getContext().watch(reference);
      users.add(reference);
    }
    logEvent(CreateUsersEnd.class, CreateUsersEnd::new);
    return users;
  }

  private void sendContacts(List<ActorRef<UserMessage>> users) {
    logEvent(SendContactsStart.class, SendContactsStart::new);
    for (var edge : contactNetwork.edgeSet()) {
      var user1 = users.get(contactNetwork.getEdgeSource(edge));
      var user2 = users.get(contactNetwork.getEdgeTarget(edge));
      user1.tell(new ContactMessage(user2, edge.getTime(), parameters.contactExpiry()));
      user2.tell(new ContactMessage(user1, edge.getTime(), parameters.contactExpiry()));
    }
    logEvent(SendContactsEnd.class, SendContactsEnd::new);
  }

  private void sendRiskScores(List<ActorRef<UserMessage>> users) {
    logEvent(SendRiskScoresStart.class, SendRiskScoresStart::new);
    for (var i = 0; i < userCount; i++) {
      var user = users.get(i);
      var score = scoreFactory.getRiskScore(i);
      user.tell(new RiskScoreMessage(user, score));
    }
    logEvent(SendRiskScoresEnd.class, SendRiskScoresEnd::new);
  }

  private <T extends Event> void logEvent(Class<T> type, Function<Instant, T> factory) {
    logger.log(type, () -> factory.apply(context.timeSource().instant()));
  }
}

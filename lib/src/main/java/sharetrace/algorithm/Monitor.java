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
import java.util.HashMap;
import java.util.Map;
import sharetrace.graph.TemporalNetwork;
import sharetrace.logging.metric.CreateUsersRuntime;
import sharetrace.logging.metric.MessagePassingRuntime;
import sharetrace.logging.metric.MetricRecord;
import sharetrace.logging.metric.SendContactsRuntime;
import sharetrace.logging.metric.SendRiskScoresRuntime;
import sharetrace.logging.metric.TotalRuntime;
import sharetrace.model.Parameters;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.IdleTimeout;
import sharetrace.model.message.MonitorMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.Run;
import sharetrace.model.message.UserMessage;
import sharetrace.util.Context;
import sharetrace.util.Timer;

// TODO Consider setting node attribute to be the exposure score and export before/after
final class Monitor<K> extends AbstractBehavior<MonitorMessage> {

  private final Context context;
  private final Parameters parameters;
  private final RiskScoreFactory<K> scoreFactory;
  private final TemporalNetwork<K> contactNetwork;
  private final int userCount;
  private final Timer<Class<? extends MetricRecord>> timer;
  private final BitSet timeouts;

  private Monitor(
      ActorContext<MonitorMessage> ctx,
      Context context,
      Parameters parameters,
      RiskScoreFactory<K> scoreFactory,
      TemporalNetwork<K> contactNetwork) {
    super(ctx);
    this.context = context;
    this.parameters = parameters;
    this.scoreFactory = scoreFactory;
    this.contactNetwork = contactNetwork;
    this.userCount = contactNetwork.nodeSet().size();
    this.timer = new Timer<>();
    this.timeouts = new BitSet(userCount);
  }

  public static <K> Behavior<MonitorMessage> of(
      Context context,
      Parameters parameters,
      RiskScoreFactory<K> scoreFactory,
      TemporalNetwork<K> contactNetwork) {
    return Behaviors.setup(
        ctx -> {
          var monitor = new Monitor<>(ctx, context, parameters, scoreFactory, contactNetwork);
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

  private Behavior<MonitorMessage> handle(Run message) {
    if (userCount < 1) {
      return Behaviors.stopped();
    }
    timer.start();
    var users = timer.time(this::newUsers, CreateUsersRuntime.class);
    timer.time(() -> sendContacts(users), SendContactsRuntime.class);
    timer.time(() -> sendRiskScores(users), SendRiskScoresRuntime.class);
    return this;
  }

  private Behavior<MonitorMessage> handle(IdleTimeout timeout) {
    timeouts.set(timeout.key());
    if (timeouts.cardinality() == userCount) {
      timer.stop();
      logMetrics();
      return Behaviors.stopped();
    }
    return this;
  }

  private Map<K, ActorRef<UserMessage>> newUsers() {
    var users = new HashMap<K, ActorRef<UserMessage>>();
    var i = 0;
    for (var key : contactNetwork.nodeSet()) {
      var user = User.of(context, parameters, getContext().getSelf(), new IdleTimeout(i));
      var reference = getContext().spawn(user, String.valueOf(key), User.props());
      getContext().watch(reference);
      users.put(key, reference);
      i++;
    }
    return users;
  }

  private void sendContacts(Map<K, ActorRef<UserMessage>> users) {
    for (var edge : contactNetwork.edgeSet()) {
      var user1 = users.get(contactNetwork.getEdgeSource(edge));
      var user2 = users.get(contactNetwork.getEdgeTarget(edge));
      user1.tell(new ContactMessage(user2, edge.getTimestamp(), parameters.contactExpiry()));
      user2.tell(new ContactMessage(user1, edge.getTimestamp(), parameters.contactExpiry()));
    }
  }

  private void sendRiskScores(Map<K, ActorRef<UserMessage>> users) {
    users.forEach((key, user) -> user.tell(new RiskScoreMessage(user, scoreFactory.getScore(key))));
  }

  private void logMetrics() {
    var logger = context.metricsLogger();
    logger.log(CreateUsersRuntime.class, this::createUsersRuntime);
    logger.log(SendContactsRuntime.class, this::sendContactsRuntime);
    logger.log(SendRiskScoresRuntime.class, this::sendRiskScoresRuntime);
    logger.log(TotalRuntime.class, this::totalRuntime);
    logger.log(MessagePassingRuntime.class, this::messagePassingRuntime);
  }

  private CreateUsersRuntime createUsersRuntime() {
    return new CreateUsersRuntime(timer.duration(CreateUsersRuntime.class));
  }

  private SendContactsRuntime sendContactsRuntime() {
    return new SendContactsRuntime(timer.duration(SendContactsRuntime.class));
  }

  private SendRiskScoresRuntime sendRiskScoresRuntime() {
    return new SendRiskScoresRuntime(timer.duration(SendRiskScoresRuntime.class));
  }

  private TotalRuntime totalRuntime() {
    return new TotalRuntime(timer.duration(TotalRuntime.class));
  }

  private MessagePassingRuntime messagePassingRuntime() {
    var total = timer.duration(TotalRuntime.class);
    var createUsers = timer.duration(CreateUsersRuntime.class);
    var sendUsers = timer.duration(SendContactsRuntime.class);
    var exclude = createUsers.plus(sendUsers);
    return new MessagePassingRuntime(total.minus(exclude));
  }
}

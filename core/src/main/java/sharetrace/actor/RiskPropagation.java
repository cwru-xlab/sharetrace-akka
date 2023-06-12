package sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.Props;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import java.time.Clock;
import java.time.Duration;
import java.util.BitSet;
import java.util.Map;
import java.util.function.Supplier;
import org.immutables.builder.Builder;
import sharetrace.experiment.data.IntervalCacheFactory;
import sharetrace.experiment.data.RiskScoreFactory;
import sharetrace.graph.TemporalEdge;
import sharetrace.graph.TemporalNetwork;
import sharetrace.model.RiskScore;
import sharetrace.model.UserParameters;
import sharetrace.model.message.AlgorithmMessage;
import sharetrace.model.message.ContactMessage;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.model.message.RunMessage;
import sharetrace.model.message.TimedOutMessage;
import sharetrace.model.message.UserMessage;
import sharetrace.util.Collecting;
import sharetrace.util.logging.Logging;
import sharetrace.util.logging.RecordLogger;
import sharetrace.util.logging.metric.CreateUsersRuntime;
import sharetrace.util.logging.metric.MessagePassingRuntime;
import sharetrace.util.logging.metric.MetricRecord;
import sharetrace.util.logging.metric.RiskPropagationRuntime;
import sharetrace.util.logging.metric.SendContactsRuntime;
import sharetrace.util.logging.metric.SendRiskScoresRuntime;

// TODO Add support to initialize all contacts up front
final class RiskPropagation<K> extends AbstractBehavior<AlgorithmMessage> {

  private static final RecordLogger<MetricRecord> LOGGER = Logging.metricsLogger();

  private final UserParameters userParameters;
  private final IntervalCacheFactory<RiskScoreMessage> cacheFactory;
  private final RiskScoreFactory<K> scoreFactory;
  private final TemporalNetwork<K> contactNetwork;
  private final int userCount;
  private final Clock clock;
  private final Timer<Class<? extends MetricRecord>> timer;
  private final BitSet timeouts;

  private RiskPropagation(
      ActorContext<AlgorithmMessage> context,
      RiskScoreFactory<K> scoreFactory,
      TemporalNetwork<K> contactNetwork,
      UserParameters userParameters,
      IntervalCacheFactory<RiskScoreMessage> cacheFactory,
      Clock clock) {
    super(context);
    this.scoreFactory = scoreFactory;
    this.contactNetwork = contactNetwork;
    this.userCount = contactNetwork.nodeSet().size();
    this.userParameters = userParameters;
    this.cacheFactory = cacheFactory;
    this.clock = clock;
    this.timer = new Timer<>();
    this.timeouts = new BitSet(userCount);
  }

  private static <T extends MetricRecord> void logMetric(Class<T> type, Supplier<T> metric) {
    LOGGER.log(MetricRecord.KEY, type, metric);
  }

  @Builder.Factory
  static <T> Algorithm riskPropagation(
      RiskScoreFactory<T> scoreFactory,
      TemporalNetwork<T> contactNetwork,
      UserParameters userParameters,
      IntervalCacheFactory<RiskScoreMessage> cacheFactory,
      Clock clock) {
    return Algorithm.builder()
        .name(RiskPropagation.class.getSimpleName())
        .properties(DispatcherSelector.fromConfig("sharetrace.risk-propagation.dispatcher"))
        .behavior(
            Behaviors.setup(
                context ->
                    Behaviors.withMdc(
                        AlgorithmMessage.class,
                        Logging.getMdc(),
                        new RiskPropagation<>(
                            context,
                            scoreFactory,
                            contactNetwork,
                            userParameters,
                            cacheFactory,
                            clock))))
        .build();
  }

  @Override
  public Receive<AlgorithmMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(RunMessage.class, this::handle)
        .onMessage(TimedOutMessage.class, this::handle)
        .build();
  }

  private Behavior<AlgorithmMessage> handle(RunMessage message) {
    Behavior<AlgorithmMessage> behavior = this;
    if (userCount > 0) {
      timer.start();
      Map<K, ActorRef<UserMessage>> users = timer.time(this::newUsers, CreateUsersRuntime.class);
      timer.time(() -> sendContacts(users), SendContactsRuntime.class);
      timer.time(() -> sendRiskScores(users), SendRiskScoresRuntime.class);
    } else {
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Behavior<AlgorithmMessage> handle(TimedOutMessage message) {
    Behavior<AlgorithmMessage> behavior = this;
    timeouts.set(message.timeoutId());
    if (timeouts.cardinality() == userCount) {
      timer.stop();
      logMetrics();
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Map<K, ActorRef<UserMessage>> newUsers() {
    Map<K, ActorRef<UserMessage>> users = Collecting.newHashMap(userCount);
    Props properties = DispatcherSelector.fromConfig("sharetrace.user.dispatcher");
    int timeoutId = 0;
    for (K key : contactNetwork.nodeSet()) {
      ActorRef<UserMessage> user = newUser(timeoutId, key, properties);
      getContext().watch(user);
      users.put(key, user);
      timeoutId++;
    }
    return users;
  }

  private ActorRef<UserMessage> newUser(int timeoutId, K key, Props properties) {
    return getContext().spawn(newUser(timeoutId), String.valueOf(key), properties);
  }

  private Behavior<UserMessage> newUser(int timeoutId) {
    return UserBuilder.create()
        .coordinator(getContext().getSelf())
        .timeoutId(timeoutId)
        .userParameters(userParameters)
        .clock(clock)
        .cache(cacheFactory.newCache())
        .build();
  }

  private void sendRiskScores(Map<K, ActorRef<UserMessage>> users) {
    for (Map.Entry<K, ActorRef<UserMessage>> entry : users.entrySet()) {
      K key = entry.getKey();
      ActorRef<UserMessage> user = entry.getValue();
      RiskScore score = scoreFactory.getScore(key);
      user.tell(RiskScoreMessage.of(score, user));
    }
  }

  private void sendContacts(Map<K, ActorRef<UserMessage>> users) {
    for (TemporalEdge edge : contactNetwork.edgeSet()) {
      ActorRef<UserMessage> user1 = users.get(contactNetwork.getEdgeSource(edge));
      ActorRef<UserMessage> user2 = users.get(contactNetwork.getEdgeTarget(edge));
      user1.tell(ContactMessage.of(user2, edge.getTimestamp()));
      user2.tell(ContactMessage.of(user1, edge.getTimestamp()));
    }
  }

  private void logMetrics() {
    logMetric(CreateUsersRuntime.class, this::createUsersRuntime);
    logMetric(SendRiskScoresRuntime.class, this::sendRiskScoresRuntime);
    logMetric(SendContactsRuntime.class, this::sendContactsRuntime);
    logMetric(RiskPropagationRuntime.class, this::riskPropagationRuntime);
    logMetric(MessagePassingRuntime.class, this::messagePassingRuntime);
  }

  private CreateUsersRuntime createUsersRuntime() {
    return CreateUsersRuntime.of(timer.duration(CreateUsersRuntime.class));
  }

  private SendRiskScoresRuntime sendRiskScoresRuntime() {
    return SendRiskScoresRuntime.of(timer.duration(SendRiskScoresRuntime.class));
  }

  private SendContactsRuntime sendContactsRuntime() {
    return SendContactsRuntime.of(timer.duration(SendContactsRuntime.class));
  }

  private RiskPropagationRuntime riskPropagationRuntime() {
    return RiskPropagationRuntime.of(timer.duration(RiskPropagationRuntime.class));
  }

  private MessagePassingRuntime messagePassingRuntime() {
    Duration total = timer.duration(RiskPropagationRuntime.class);
    Duration sendRiskScoresRuntime = timer.duration(SendRiskScoresRuntime.class);
    Duration createUsersRuntime = timer.duration(CreateUsersRuntime.class);
    Duration exclude = sendRiskScoresRuntime.plus(createUsersRuntime);
    return MessagePassingRuntime.of(total.minus(exclude));
  }
}

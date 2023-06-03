package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.Props;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import io.sharetrace.experiment.data.IntervalCacheFactory;
import io.sharetrace.experiment.data.RiskScoreFactory;
import io.sharetrace.graph.TemporalEdge;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.UserParameters;
import io.sharetrace.model.message.AlgorithmMessage;
import io.sharetrace.model.message.ContactMessage;
import io.sharetrace.model.message.RiskScoreMessage;
import io.sharetrace.model.message.RunMessage;
import io.sharetrace.model.message.TimedOutMessage;
import io.sharetrace.model.message.UserMessage;
import io.sharetrace.util.Collecting;
import io.sharetrace.util.Indexer;
import io.sharetrace.util.logging.Logging;
import io.sharetrace.util.logging.RecordLogger;
import io.sharetrace.util.logging.metric.CreateUsersRuntime;
import io.sharetrace.util.logging.metric.MessagePassingRuntime;
import io.sharetrace.util.logging.metric.MetricRecord;
import io.sharetrace.util.logging.metric.RiskPropagationRuntime;
import io.sharetrace.util.logging.metric.SendContactsRuntime;
import io.sharetrace.util.logging.metric.SendRiskScoresRuntime;
import java.time.Clock;
import java.time.Duration;
import java.util.BitSet;
import java.util.Map;
import java.util.function.Supplier;
import org.immutables.builder.Builder;
import org.jgrapht.Graph;

// TODO Add support to initialize all contacts up front
public final class RiskPropagation<T> extends AbstractBehavior<AlgorithmMessage> {

  private static final RecordLogger<MetricRecord> LOGGER = Logging.metricsLogger();

  private final UserParameters userParameters;
  private final IntervalCacheFactory<RiskScoreMessage> cacheFactory;
  private final RiskScoreFactory<T> scoreFactory;
  private final Graph<T, TemporalEdge> contactNetwork;
  private final int userCount;
  private final Clock clock;
  private final Timer<Class<? extends MetricRecord>> timer;
  private final BitSet timeouts;

  private RiskPropagation(
      ActorContext<AlgorithmMessage> context,
      RiskScoreFactory<T> scoreFactory,
      Graph<T, TemporalEdge> contactNetwork,
      UserParameters userParameters,
      IntervalCacheFactory<RiskScoreMessage> cacheFactory,
      Clock clock) {
    super(context);
    this.scoreFactory = scoreFactory;
    this.contactNetwork = contactNetwork;
    this.userCount = contactNetwork.vertexSet().size();
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
      Graph<T, TemporalEdge> contactNetwork,
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
      Map<T, ActorRef<UserMessage>> users = timer.time(this::newUsers, CreateUsersRuntime.class);
      timer.time(() -> sendRiskScores(users), SendRiskScoresRuntime.class);
      timer.time(() -> sendContacts(users), SendContactsRuntime.class);
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

  private Map<T, ActorRef<UserMessage>> newUsers() {
    Map<T, ActorRef<UserMessage>> users = Collecting.newHashMap(userCount);
    Props properties = DispatcherSelector.fromConfig("sharetrace.user.dispatcher");
    Indexer<T> indexer = new Indexer<>();
    for (T key : contactNetwork.vertexSet()) {
      int timeoutId = indexer.index(key);
      ActorRef<UserMessage> user = newUser(timeoutId, key, properties);
      getContext().watch(user);
      users.put(key, user);
    }
    return users;
  }

  private ActorRef<UserMessage> newUser(int timeoutId, T key, Props properties) {
    return getContext().spawn(newUser(timeoutId), String.valueOf(key), properties);
  }

  private Behavior<UserMessage> newUser(int timeoutId) {
    return UserBuilder.create()
        .riskPropagation(getContext().getSelf())
        .timeoutId(timeoutId)
        .userParameters(userParameters)
        .clock(clock)
        .cache(cacheFactory.newCache())
        .build();
  }

  private void sendRiskScores(Map<T, ActorRef<UserMessage>> users) {
    for (Map.Entry<T, ActorRef<UserMessage>> entry : users.entrySet()) {
      T key = entry.getKey();
      ActorRef<UserMessage> user = entry.getValue();
      RiskScore score = scoreFactory.getScore(key);
      user.tell(RiskScoreMessage.of(score, user));
    }
  }

  private void sendContacts(Map<T, ActorRef<UserMessage>> users) {
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

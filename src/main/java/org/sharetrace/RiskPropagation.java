package org.sharetrace;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.builder.Builder;
import org.sharetrace.data.factory.CacheFactory;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.ScoreFactory;
import org.sharetrace.graph.ContactGraph;
import org.sharetrace.graph.Node;
import org.sharetrace.graph.NodeBuilder;
import org.sharetrace.graph.TemporalGraph;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.Loggables;
import org.sharetrace.logging.Logging;
import org.sharetrace.logging.metrics.LoggableMetric;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.message.AlgorithmMessage;
import org.sharetrace.message.ContactMessage;
import org.sharetrace.message.NodeMessage;
import org.sharetrace.message.NodeParameters;
import org.sharetrace.message.RiskScoreMessage;
import org.sharetrace.message.Run;
import org.sharetrace.util.TypedSupplier;

/**
 * A non-iterative, asynchronous implementation of the ShareTrace algorithm. The objective is to
 * estimate the marginal posterior probability of infection for all individuals in the specified
 * {@link ContactGraph}. The main steps of the algorithm are as follows:
 *
 * <ol>
 *   <li>Map the {@link ContactGraph} to a collection {@link Node} actors.
 *   <li>For each {@link Node}, send it an initial {@link RiskScoreMessage}.
 *   <li>For each pair of {@link Node}s that correspond to an edge in the {@link ContactGraph}, send
 *       each a complimentary {@link ContactMessage} that contains the {@link ActorRef} and time of
 *       contact of the other {@link Node}.
 *   <li>Terminate once the stopping condition is satisfied. Termination occurs when when all {@link
 *       Node}s have stopped passing messages (default), or a certain amount of time has passed.
 * </ol>
 *
 * @see NodeParameters
 */
public class RiskPropagation extends AbstractBehavior<AlgorithmMessage> {

  private final Loggables loggables;
  private final NodeParameters parameters;
  private final TemporalGraph graph;
  private final long nNodes;
  private final Clock clock;
  private final ScoreFactory scoreFactory;
  private final ContactTimeFactory contactTimeFactory;
  private final CacheFactory<RiskScoreMessage> cacheFactory;
  private Instant startedAt;
  private int nStopped;

  private RiskPropagation(
      ActorContext<AlgorithmMessage> context,
      Set<Class<? extends Loggable>> loggable,
      TemporalGraph graph,
      NodeParameters parameters,
      Clock clock,
      CacheFactory<RiskScoreMessage> cacheFactory,
      ScoreFactory scoreFactory,
      ContactTimeFactory contactTimeFactory) {
    super(context);
    this.loggables = Loggables.create(loggable, () -> getContext().getLog());
    this.graph = graph;
    this.parameters = parameters;
    this.clock = clock;
    this.cacheFactory = cacheFactory;
    this.scoreFactory = scoreFactory;
    this.contactTimeFactory = contactTimeFactory;
    this.nNodes = graph.nNodes();
    this.nStopped = 0;
  }

  @Builder.Factory
  static Behavior<AlgorithmMessage> riskPropagation(
      TemporalGraph graph,
      Set<Class<? extends Loggable>> loggable,
      NodeParameters parameters,
      Clock clock,
      CacheFactory<RiskScoreMessage> cacheFactory,
      ScoreFactory scoreFactory,
      ContactTimeFactory contactTimeFactory) {
    return Behaviors.setup(
        context -> {
          context.setLoggerName(Logging.METRIC_LOGGER_NAME);
          return new RiskPropagation(
              context,
              loggable,
              graph,
              parameters,
              clock,
              cacheFactory,
              scoreFactory,
              contactTimeFactory);
        });
  }

  @Override
  public Receive<AlgorithmMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(Run.class, this::onRun)
        .onSignal(Terminated.class, this::onTerminate)
        .build();
  }

  private Behavior<AlgorithmMessage> onRun(Run run) {
    Behavior<AlgorithmMessage> behavior = this;
    if (nNodes > 0) {
      Map<Integer, ActorRef<NodeMessage>> nodes = newNodes();
      startedAt = clock.instant();
      setScores(nodes);
      setContacts(nodes);
    } else {
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Behavior<AlgorithmMessage> onTerminate(Terminated terminated) {
    Behavior<AlgorithmMessage> behavior = this;
    if (++nStopped == nNodes) {
      logMetrics();
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Map<Integer, ActorRef<NodeMessage>> newNodes() {
    Map<Integer, ActorRef<NodeMessage>> nodes = new Object2ObjectOpenHashMap<>();
    graph.nodes().forEach(name -> nodes.put(name, newNode(name)));
    return nodes;
  }

  private void setScores(Map<Integer, ActorRef<NodeMessage>> nodes) {
    nodes.forEach(this::sendFirstScore);
  }

  private void setContacts(Map<?, ActorRef<NodeMessage>> nodes) {
    graph.edges().forEach(edge -> sendContact(edge, nodes));
  }

  private void logMetrics() {
    loggables.info(LoggableMetric.KEY, runtimeMetric());
  }

  private ActorRef<NodeMessage> newNode(int name) {
    ActorRef<NodeMessage> node = getContext().spawn(newNode(), String.valueOf(name));
    getContext().watch(node);
    return node;
  }

  private void sendFirstScore(int name, ActorRef<NodeMessage> node) {
    node.tell(RiskScoreMessage.builder().score(scoreFactory.getScore(name)).replyTo(node).build());
  }

  private void sendContact(List<Integer> edge, Map<?, ActorRef<NodeMessage>> nodes) {
    ActorRef<NodeMessage> node1 = nodes.get(edge.get(0));
    ActorRef<NodeMessage> node2 = nodes.get(edge.get(1));
    Instant timestamp = contactTimeFactory.getContactTime(edge.get(0), edge.get(1));
    node1.tell(ContactMessage.builder().replyTo(node2).timestamp(timestamp).build());
    node2.tell(ContactMessage.builder().replyTo(node1).timestamp(timestamp).build());
  }

  private TypedSupplier<LoggableMetric> runtimeMetric() {
    return TypedSupplier.of(RuntimeMetric.class, () -> RuntimeMetric.of(runtime()));
  }

  private Behavior<NodeMessage> newNode() {
    return Behaviors.withTimers(
        timers ->
            NodeBuilder.create()
                .timers(timers)
                .addAllLoggable(loggables.loggable())
                .parameters(parameters)
                .clock(clock)
                .cache(cacheFactory.create())
                .build());
  }

  private double runtime() {
    return Duration.between(startedAt, clock.instant()).toNanos() / 1e9;
  }
}

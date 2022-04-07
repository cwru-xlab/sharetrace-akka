package org.sharetrace;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.immutables.builder.Builder;
import org.sharetrace.model.graph.ContactGraph;
import org.sharetrace.model.graph.Node;
import org.sharetrace.model.graph.NodeBuilder;
import org.sharetrace.model.graph.TemporalGraph;
import org.sharetrace.model.message.AlgorithmMessage;
import org.sharetrace.model.message.Contact;
import org.sharetrace.model.message.NodeMessage;
import org.sharetrace.model.message.Parameters;
import org.sharetrace.model.message.RiskScore;
import org.sharetrace.model.message.Run;
import org.sharetrace.util.IntervalCache;

// TODO Update ContactGraph
/**
 * A non-iterative, asynchronous implementation of the ShareTrace algorithm. The objective is to
 * estimate the marginal posterior probability of infection for all individuals in the specified
 * {@link ContactGraph}. The main steps of the algorithm are as follows:
 *
 * <ol>
 *   <li>Map the {@link ContactGraph} to a collection {@link Node} actors.
 *   <li>For each {@link Node}, send it an initial {@link RiskScore} message.
 *   <li>For each pair of {@link Node}s that correspond to an edge in the {@link ContactGraph}, send
 *       each a complimentary {@link Contact} message that contains the {@link ActorRef} and time of
 *       contact of the other {@link Node}.
 *   <li>Terminate once the stopping condition is satisfied. Termination occurs when when all {@link
 *       Node}s have stopped passing messages (default), or a certain amount of time has passed.
 * </ol>
 *
 * @see Parameters
 */
public class RiskPropagation<T> extends AbstractBehavior<AlgorithmMessage> {

  private final Parameters parameters;
  private final TemporalGraph<T> graph;
  private final int nNodes;
  private final Supplier<Instant> clock;
  private final Function<T, RiskScore> scoreFactory;
  private final BiFunction<T, T, Instant> timeFactory;
  private final Supplier<IntervalCache<RiskScore>> cacheFactory;
  private final Duration nodeTimeout;
  private final Duration nodeRefreshRate;
  private Instant startedAt;
  private int nStopped;

  private RiskPropagation(
      ActorContext<AlgorithmMessage> context,
      TemporalGraph<T> graph,
      Parameters parameters,
      Supplier<Instant> clock,
      Supplier<IntervalCache<RiskScore>> cacheFactory,
      Function<T, RiskScore> scoreFactory,
      BiFunction<T, T, Instant> timeFactory,
      Duration nodeTimeout,
      Duration nodeRefreshRate) {
    super(context);
    this.graph = graph;
    this.parameters = parameters;
    this.clock = clock;
    this.cacheFactory = cacheFactory;
    this.scoreFactory = scoreFactory;
    this.timeFactory = timeFactory;
    this.nodeTimeout = nodeTimeout;
    this.nodeRefreshRate = nodeRefreshRate;
    this.nNodes = graph.nodes().size();
    this.nStopped = 0;
  }

  @Builder.Factory
  protected static <T> Behavior<AlgorithmMessage> riskPropagation(
      TemporalGraph<T> graph,
      Duration nodeTimeout,
      Duration nodeRefreshRate,
      Parameters parameters,
      Supplier<Instant> clock,
      Supplier<IntervalCache<RiskScore>> cacheFactory,
      Function<T, RiskScore> scoreFactory,
      BiFunction<T, T, Instant> timeFactory) {
    return Behaviors.setup(
        context ->
            new RiskPropagation<>(
                context,
                graph,
                parameters,
                clock,
                cacheFactory,
                scoreFactory,
                timeFactory,
                nodeTimeout,
                nodeRefreshRate));
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
      Map<T, ActorRef<NodeMessage>> nodes = newNodes();
      startedAt = clock.get();
      setScores(nodes);
      setContacts(nodes);
    } else {
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Map<T, ActorRef<NodeMessage>> newNodes() {
    return graph.nodes().stream()
        .map(name -> Map.entry(name, newNode(name)))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private ActorRef<NodeMessage> newNode(T name) {
    ActorRef<NodeMessage> node = getContext().spawn(newNode(), String.valueOf(name));
    getContext().watch(node);
    return node;
  }

  private Behavior<NodeMessage> newNode() {
    return Behaviors.withTimers(
        timers ->
            NodeBuilder.create()
                .timers(timers)
                .parameters(parameters)
                .clock(clock)
                .cache(cacheFactory.get())
                .idleTimeout(nodeTimeout)
                .refreshRate(nodeRefreshRate)
                .build());
  }

  private Behavior<AlgorithmMessage> onTerminate(Terminated terminated) {
    Behavior<AlgorithmMessage> behavior = this;
    if (++nStopped == nNodes) {
      // Do not include "PT" of Duration string.
      String runtime = Duration.between(startedAt, clock.get()).toString().substring(2);
      getContext().getLog().info("Runtime: {}", runtime);
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private void onEdge(List<T> edge, Map<?, ActorRef<NodeMessage>> nodes) {
    ActorRef<NodeMessage> node1 = nodes.get(edge.get(0));
    ActorRef<NodeMessage> node2 = nodes.get(edge.get(1));
    Instant timestamp = timeFactory.apply(edge.get(0), edge.get(1));
    node1.tell(Contact.builder().replyTo(node2).timestamp(timestamp).build());
    node2.tell(Contact.builder().replyTo(node1).timestamp(timestamp).build());
  }

  private void setScores(Map<T, ActorRef<NodeMessage>> nodes) {
    nodes.forEach((name, node) -> node.tell(scoreFactory.apply(name)));
  }

  private void setContacts(Map<?, ActorRef<NodeMessage>> nodes) {
    graph.edges().forEach(edge -> onEdge(edge, nodes));
  }
}

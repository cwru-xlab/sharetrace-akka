package org.sharetrace;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import org.immutables.builder.Builder;
import org.sharetrace.model.graph.ContactGraph;
import org.sharetrace.model.graph.Node;
import org.sharetrace.model.graph.NodeBuilder;
import org.sharetrace.model.graph.TemporalGraph;
import org.sharetrace.model.message.AlgorithmMessage;
import org.sharetrace.model.message.ContactMessage;
import org.sharetrace.model.message.NodeMessage;
import org.sharetrace.model.message.Parameters;
import org.sharetrace.model.message.RiskScore;
import org.sharetrace.model.message.RiskScoreMessage;
import org.sharetrace.model.message.Run;
import org.sharetrace.util.EventLog;
import org.sharetrace.util.IntervalCache;
import org.sharetrace.util.NodeEvent;

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
 * @see Parameters
 */
public class RiskPropagation<T> extends AbstractBehavior<AlgorithmMessage> {

  private final EventLog<NodeEvent> log;
  private final Parameters parameters;
  private final TemporalGraph<T> graph;
  private final int nNodes;
  private final Supplier<Instant> clock;
  private final Function<T, RiskScore> scoreFactory;
  private final BiFunction<T, T, Instant> timeFactory;
  private final Supplier<IntervalCache<RiskScoreMessage>> cacheFactory;
  private final BiFunction<RiskScore, Parameters, RiskScore> transmitter;
  private Instant startedAt;
  private int nStopped;

  private RiskPropagation(
      ActorContext<AlgorithmMessage> context,
      EventLog<NodeEvent> log,
      TemporalGraph<T> graph,
      Parameters parameters,
      BiFunction<RiskScore, Parameters, RiskScore> transmitter,
      Supplier<Instant> clock,
      Supplier<IntervalCache<RiskScoreMessage>> cacheFactory,
      Function<T, RiskScore> scoreFactory,
      BiFunction<T, T, Instant> timeFactory) {
    super(context);
    this.log = log;
    this.graph = graph;
    this.parameters = parameters;
    this.transmitter = transmitter;
    this.clock = clock;
    this.cacheFactory = cacheFactory;
    this.scoreFactory = scoreFactory;
    this.timeFactory = timeFactory;
    this.nNodes = graph.nodes().size();
    this.nStopped = 0;
  }

  @Builder.Factory
  protected static <T> Behavior<AlgorithmMessage> riskPropagation(
      TemporalGraph<T> graph,
      EventLog<NodeEvent> log,
      Parameters parameters,
      BiFunction<RiskScore, Parameters, RiskScore> transmitter,
      Supplier<Instant> clock,
      Supplier<IntervalCache<RiskScoreMessage>> cacheFactory,
      Function<T, RiskScore> scoreFactory,
      BiFunction<T, T, Instant> timeFactory) {
    return Behaviors.setup(
        context ->
            new RiskPropagation<>(
                context,
                log,
                graph,
                parameters,
                transmitter,
                clock,
                cacheFactory,
                scoreFactory,
                timeFactory));
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
    Map<T, ActorRef<NodeMessage>> nodes = new Object2ObjectOpenHashMap<>();
    graph.nodes().forEach(name -> nodes.put(name, newNode(name)));
    return nodes;
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
                .log(log)
                .parameters(parameters)
                .transmitter(transmitter)
                .clock(clock)
                .cache(cacheFactory.get())
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

  private void sendContact(List<T> edge, Map<?, ActorRef<NodeMessage>> nodes) {
    ActorRef<NodeMessage> node1 = nodes.get(edge.get(0));
    ActorRef<NodeMessage> node2 = nodes.get(edge.get(1));
    Instant timestamp = timeFactory.apply(edge.get(0), edge.get(1));
    node1.tell(ContactMessage.builder().replyTo(node2).timestamp(timestamp).build());
    node2.tell(ContactMessage.builder().replyTo(node1).timestamp(timestamp).build());
  }

  private void sendFirstScore(T name, ActorRef<NodeMessage> node) {
    node.tell(RiskScoreMessage.builder().score(scoreFactory.apply(name)).replyTo(node).build());
  }

  private void setScores(Map<T, ActorRef<NodeMessage>> nodes) {
    nodes.forEach(this::sendFirstScore);
  }

  private void setContacts(Map<?, ActorRef<NodeMessage>> nodes) {
    graph.edges().forEach(edge -> sendContact(edge, nodes));
  }
}

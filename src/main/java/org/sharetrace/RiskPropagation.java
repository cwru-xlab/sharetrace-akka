package org.sharetrace;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.immutables.builder.Builder;
import org.sharetrace.model.graph.ContactGraph;
import org.sharetrace.model.graph.Edge;
import org.sharetrace.model.graph.Node;
import org.sharetrace.model.graph.NodeBuilder;
import org.sharetrace.model.message.Contact;
import org.sharetrace.model.message.NodeMessage;
import org.sharetrace.model.message.Parameters;
import org.sharetrace.model.message.RiskPropMessage;
import org.sharetrace.model.message.RiskScore;
import org.sharetrace.model.message.Run;
import org.sharetrace.util.IntervalCache;

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
public class RiskPropagation extends AbstractBehavior<RiskPropMessage> {

  private final Parameters parameters;
  private final ContactGraph graph;
  private final long nNodes;
  private final Supplier<Instant> clock;
  private final Function<ActorRef<NodeMessage>, RiskScore> scoreFactory;
  private final BiFunction<ActorRef<NodeMessage>, ActorRef<NodeMessage>, Instant> timeFactory;
  private final Supplier<IntervalCache<RiskScore>> cacheFactory;
  private final Duration nodeTimeout;
  private final Instant startedAt;
  private long nStopped;

  private RiskPropagation(
      ActorContext<RiskPropMessage> context,
      ContactGraph graph,
      Parameters parameters,
      Supplier<Instant> clock,
      Supplier<IntervalCache<RiskScore>> cacheFactory,
      Function<ActorRef<NodeMessage>, RiskScore> scoreFactory,
      BiFunction<ActorRef<NodeMessage>, ActorRef<NodeMessage>, Instant> timeFactory,
      Duration nodeTimeout) {
    super(context);
    this.graph = graph;
    this.parameters = parameters;
    this.clock = clock;
    this.cacheFactory = cacheFactory;
    this.scoreFactory = scoreFactory;
    this.timeFactory = timeFactory;
    this.nodeTimeout = nodeTimeout;
    this.nNodes = graph.vertexSet().size();
    this.startedAt = clock.get();
    this.nStopped = 0L;
  }

  @Builder.Factory
  protected static Behavior<RiskPropMessage> riskPropagation(
      ContactGraph graph,
      Duration nodeTimeout,
      Parameters parameters,
      Supplier<Instant> clock,
      Supplier<IntervalCache<RiskScore>> cacheFactory,
      Function<ActorRef<NodeMessage>, RiskScore> scoreFactory,
      BiFunction<ActorRef<NodeMessage>, ActorRef<NodeMessage>, Instant> timeFactory) {
    return Behaviors.setup(
        context ->
            new RiskPropagation(
                context,
                graph,
                parameters,
                clock,
                cacheFactory,
                scoreFactory,
                timeFactory,
                nodeTimeout));
  }

  @Override
  public Receive<RiskPropMessage> createReceive() {
    return newReceiveBuilder()
        .onMessage(Run.class, this::onRun)
        .onSignal(Terminated.class, this::onTerminate)
        .build();
  }

  private Behavior<RiskPropMessage> onRun(Run run) {
    Behavior<RiskPropMessage> behavior = this;
    if (nNodes > 0) {
      Map<Long, ActorRef<NodeMessage>> nodes = newNodes();
      setScores(nodes);
      setContacts(nodes);
    } else {
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private Map<Long, ActorRef<NodeMessage>> newNodes() {
    return graph.vertexSet().stream()
        .map(name -> Map.entry(name, newNode(name)))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private ActorRef<NodeMessage> newNode(long name) {
    ActorRef<NodeMessage> node =
        getContext().spawn(Behaviors.withTimers(this::newNode), String.valueOf(name));
    getContext().watch(node);
    return node;
  }

  private Behavior<NodeMessage> newNode(TimerScheduler<NodeMessage> timers) {
    return NodeBuilder.create()
        .timers(timers)
        .parameters(parameters)
        .clock(clock)
        .cache(cacheFactory.get())
        .idleTimeout(nodeTimeout)
        .build();
  }

  private Behavior<RiskPropMessage> onTerminate(Terminated terminated) {
    Behavior<RiskPropMessage> behavior = this;
    if (++nStopped == nNodes) {
      // Do not include "PT" of Duration string.
      String runtime = Duration.between(startedAt, clock.get()).toString().substring(2);
      getContext().getLog().info("Runtime: {}", runtime);
      behavior = Behaviors.stopped();
    }
    return behavior;
  }

  private void setScores(Map<Long, ActorRef<NodeMessage>> nodes) {
    nodes.values().forEach(node -> node.tell(scoreFactory.apply(node)));
  }

  private void setContacts(Map<Long, ActorRef<NodeMessage>> nodes) {
    ActorRef<NodeMessage> source, target;
    Instant timestamp;
    for (Edge<Long> edge : graph.edgeSet()) {
      source = nodes.get(edge.source());
      target = nodes.get(edge.target());
      timestamp = timeFactory.apply(source, target);
      source.tell(Contact.builder().replyTo(target).timestamp(timestamp).build());
      target.tell(Contact.builder().replyTo(source).timestamp(timestamp).build());
    }
  }
}

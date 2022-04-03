package org.sharetrace;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
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
import org.sharetrace.model.graph.NodeBuilder;
import org.sharetrace.model.message.Contact;
import org.sharetrace.model.message.NodeMessage;
import org.sharetrace.model.message.Parameters;
import org.sharetrace.model.message.RiskPropagationMessage;
import org.sharetrace.model.message.RiskScore;
import org.sharetrace.model.message.Run;
import org.sharetrace.util.IntervalCache;

/** A non-iterative, asynchronous implementation of the ShareTrace algorithm. */
public class RiskPropagation extends AbstractBehavior<RiskPropagationMessage> {

  private final Parameters parameters;
  private final ContactGraph graph;
  private final Supplier<Instant> clock;
  private final Function<ActorRef<NodeMessage>, RiskScore> scoreFactory;
  private final BiFunction<ActorRef<NodeMessage>, ActorRef<NodeMessage>, Instant> timeFactory;
  private final Supplier<IntervalCache<RiskScore>> cacheFactory;

  private RiskPropagation(
      ActorContext<RiskPropagationMessage> context,
      ContactGraph graph,
      Parameters parameters,
      Supplier<Instant> clock,
      Supplier<IntervalCache<RiskScore>> cacheFactory,
      Function<ActorRef<NodeMessage>, RiskScore> scoreFactory,
      BiFunction<ActorRef<NodeMessage>, ActorRef<NodeMessage>, Instant> timeFactory) {
    super(context);
    this.graph = graph;
    this.parameters = parameters;
    this.clock = clock;
    this.cacheFactory = cacheFactory;
    this.scoreFactory = scoreFactory;
    this.timeFactory = timeFactory;
  }

  @Builder.Factory
  protected static Behavior<RiskPropagationMessage> riskPropagation(
      ContactGraph graph,
      Parameters parameters,
      Supplier<Instant> clock,
      Supplier<IntervalCache<RiskScore>> cacheFactory,
      Function<ActorRef<NodeMessage>, RiskScore> scoreFactory,
      BiFunction<ActorRef<NodeMessage>, ActorRef<NodeMessage>, Instant> timeFactory) {
    return Behaviors.setup(
        context ->
            new RiskPropagation(
                context, graph, parameters, clock, cacheFactory, scoreFactory, timeFactory));
  }

  @Override
  public Receive<RiskPropagationMessage> createReceive() {
    return newReceiveBuilder().onMessage(Run.class, this::onRun).build();
  }

  private Behavior<RiskPropagationMessage> onRun(Run run) {
    Map<Long, ActorRef<NodeMessage>> nodes = newNodes();
    setScores(nodes);
    setContacts(nodes);
    return this;
  }

  private Map<Long, ActorRef<NodeMessage>> newNodes() {
    return graph.vertexSet().stream()
        .map(name -> Map.entry(name, newNode(name)))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private ActorRef<NodeMessage> newNode(long name) {
    Behavior<NodeMessage> node =
        NodeBuilder.create().parameters(parameters).clock(clock).cache(cacheFactory.get()).build();
    return getContext().spawn(node, String.valueOf(name));
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

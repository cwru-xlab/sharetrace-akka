package io.sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.sharetrace.data.factory.ContactTimeFactory;
import io.sharetrace.logging.Logger;
import io.sharetrace.logging.Logging;
import io.sharetrace.logging.metric.GraphCycles;
import io.sharetrace.logging.metric.GraphEccentricity;
import io.sharetrace.logging.metric.GraphScores;
import io.sharetrace.logging.metric.GraphSize;
import io.sharetrace.logging.metric.GraphTopology;
import io.sharetrace.logging.metric.LoggableMetric;
import io.sharetrace.util.Uid;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;

@JsonIgnoreType
abstract class AbstractContactNetwork implements ContactNetwork {

  private Graph<Integer, DefaultEdge> graph;
  private Logger logger;

  protected AbstractContactNetwork() {}

  @Override
  @Value.Lazy
  public Set<Integer> users() {
    return Collections.unmodifiableSet(graph().vertexSet());
  }

  @Override
  @Value.Lazy
  public Set<Contact> contacts() {
    return graph().edgeSet().stream().map(this::contactFrom).collect(contactCollector());
  }

  @Override
  public void logMetrics() {
    GraphStats<?, ?> stats = GraphStats.of(graph());
    String key = LoggableMetric.KEY;
    logger().log(key, GraphSize.class, stats::graphSize);
    logger().log(key, GraphCycles.class, stats::graphCycles);
    logger().log(key, GraphEccentricity.class, stats::graphEccentricity);
    logger().log(key, GraphScores.class, stats::graphScores);
    if (logger().log(key, GraphTopology.class, () -> GraphTopology.of(id()))) {
      exportGraph();
    }
  }

  private Logger logger() {
    // Lazily assign to make Immutables subclassing work properly; subclass must be instantiated.
    return (logger == null) ? (logger = Logging.metricsLogger()) : logger;
  }

  @Value.Lazy
  public String id() {
    return Uid.ofIntString();
  }

  private void exportGraph() {
    Exporter.export(graph, id());
  }

  private Graph<Integer, DefaultEdge> graph() {
    // Lazily assign to make Immutables subclassing work properly; subclass must be instantiated.
    return (graph == null) ? (graph = newGraph()) : graph;
  }

  private Graph<Integer, DefaultEdge> newGraph() {
    return Graphs.newUndirectedGraph(graphGenerator());
  }

  protected abstract GraphGenerator<Integer, DefaultEdge, ?> graphGenerator();

  protected abstract ContactTimeFactory contactTimeFactory();

  private <T> Collector<T, ?, Set<T>> contactCollector() {
    int numContacts = graph().edgeSet().size();
    return Collectors.collectingAndThen(
        ObjectOpenHashSet.toSetWithExpectedSize(numContacts), ObjectSets::unmodifiable);
  }

  private Contact contactFrom(DefaultEdge edge) {
    int user1 = graph().getEdgeSource(edge);
    int user2 = graph().getEdgeTarget(edge);
    Instant contactTime = contactTimeFactory().contactTime(user1, user2);
    return Contact.builder().user1(user1).user2(user2).time(contactTime).build();
  }
}

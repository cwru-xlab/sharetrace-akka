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
import io.sharetrace.model.LoggableRef;
import io.sharetrace.util.TypedSupplier;
import io.sharetrace.util.Uid;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;

@JsonIgnoreType
abstract class AbstractContactNetwork implements ContactNetwork, LoggableRef {

  private Graph<Integer, DefaultEdge> graph;
  private Logger logger;

  protected AbstractContactNetwork() {}

  @Override
  public Set<Integer> users() {
    return Collections.unmodifiableSet(graph().vertexSet());
  }

  @Override
  public Set<Contact> contacts() {
    Set<Contact> contacts = new ObjectOpenHashSet<>(graph().edgeSet().size());
    graph().edgeSet().forEach(edge -> contacts.add(contactFrom(edge)));
    return Collections.unmodifiableSet(contacts);
  }

  @Override
  public void logMetrics() {
    GraphStats<?, ?> stats = GraphStats.of(graph());
    String key = LoggableMetric.KEY;
    logger().log(key, TypedSupplier.of(GraphSize.class, stats::graphSize));
    logger().log(key, TypedSupplier.of(GraphCycles.class, stats::graphCycles));
    logger().log(key, TypedSupplier.of(GraphEccentricity.class, stats::graphEccentricity));
    logger().log(key, TypedSupplier.of(GraphScores.class, stats::graphScores));
    String filename = Uid.ofLongString();
    if (logger().log(key, graphTopology(filename))) {
      exportGraph(filename);
    }
  }

  private static TypedSupplier<GraphTopology> graphTopology(String filename) {
    return TypedSupplier.of(GraphTopology.class, () -> GraphTopology.of(filename));
  }

  private Logger logger() {
    // Lazily assign to make Immutables subclassing work properly; subclass must be instantiated.
    return (logger == null) ? (logger = Logging.metricsLogger(loggable())) : logger;
  }

  private void exportGraph(String filename) {
    Exporter.export(graph, filename);
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

  private Contact contactFrom(DefaultEdge edge) {
    int user1 = graph().getEdgeSource(edge);
    int user2 = graph().getEdgeTarget(edge);
    Instant contactTime = contactTimeFactory().contactTime(user1, user2);
    return Contact.builder().user1(user1).user2(user2).time(contactTime).build();
  }
}

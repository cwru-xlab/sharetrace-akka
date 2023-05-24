package io.sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.sharetrace.experiment.data.factory.ContactTimeFactory;
import io.sharetrace.util.Collecting;
import io.sharetrace.util.Identifiers;
import io.sharetrace.util.logging.Logging;
import io.sharetrace.util.logging.TypedLogger;
import io.sharetrace.util.logging.metric.GraphCycles;
import io.sharetrace.util.logging.metric.GraphEccentricity;
import io.sharetrace.util.logging.metric.GraphScores;
import io.sharetrace.util.logging.metric.GraphSize;
import io.sharetrace.util.logging.metric.GraphTopology;
import io.sharetrace.util.logging.metric.LoggableMetric;
import java.time.Instant;
import java.util.Set;
import java.util.function.Supplier;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;

@JsonIgnoreType
abstract class AbstractContactNetwork implements ContactNetwork {

  private static final TypedLogger<LoggableMetric> LOGGER = Logging.metricsLogger();

  private static <T extends LoggableMetric> boolean logMetric(Class<T> type, Supplier<T> metric) {
    return LOGGER.log(LoggableMetric.KEY, type, metric);
  }

  @Override
  @Value.Derived
  public Set<Integer> users() {
    return Collecting.unmodifiable(graph().vertexSet());
  }

  @Override
  @Value.Derived
  public Set<Contact> contacts() {
    Set<DefaultEdge> edges = graph().edgeSet();
    return edges.stream().map(this::newContact).collect(Collecting.toUnmodifiableSet(edges.size()));
  }

  @Override
  public void logMetrics() {
    GraphStatistics<?, ?> statistics = GraphStatistics.of(graph());
    logMetric(GraphSize.class, statistics::graphSize);
    logMetric(GraphCycles.class, statistics::graphCycles);
    logMetric(GraphEccentricity.class, statistics::graphEccentricity);
    logMetric(GraphScores.class, statistics::graphScores);
    if (logMetric(GraphTopology.class, this::graphTopology)) {
      Exporter.export(graph(), id());
    }
  }

  private GraphTopology graphTopology() {
    return GraphTopology.of(id());
  }

  @Value.Lazy
  public String id() {
    return Identifiers.ofIntString();
  }

  private Contact newContact(DefaultEdge edge) {
    int user1 = graph().getEdgeSource(edge);
    int user2 = graph().getEdgeTarget(edge);
    Instant contactTime = contactTimeFactory().get(user1, user2);
    return Contact.builder().user1(user1).user2(user2).time(contactTime).build();
  }

  @Value.Lazy
  protected Graph<Integer, DefaultEdge> graph() {
    return Graphs.newUndirectedGraph(graphGenerator());
  }

  protected abstract GraphGenerator<Integer, DefaultEdge, ?> graphGenerator();

  protected abstract ContactTimeFactory contactTimeFactory();
}

package io.sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.sharetrace.experiment.data.factory.ContactTimeFactory;
import io.sharetrace.util.Uid;
import io.sharetrace.util.logging.Logger;
import io.sharetrace.util.logging.Logging;
import io.sharetrace.util.logging.metric.GraphCycles;
import io.sharetrace.util.logging.metric.GraphEccentricity;
import io.sharetrace.util.logging.metric.GraphScores;
import io.sharetrace.util.logging.metric.GraphSize;
import io.sharetrace.util.logging.metric.GraphTopology;
import io.sharetrace.util.logging.metric.LoggableMetric;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;

@JsonIgnoreType
abstract class AbstractContactNetwork implements ContactNetwork {

  private static final Logger LOGGER = Logging.metricsLogger();

  @Override
  @Value.Derived
  public Set<Integer> users() {
    return Collections.unmodifiableSet(graph().vertexSet());
  }

  @Override
  @Value.Derived
  public Set<Contact> contacts() {
    return graph().edgeSet().stream().map(this::contactFrom).collect(contactCollector());
  }

  @Override
  public void logMetrics() {
    GraphStats<?, ?> stats = GraphStats.of(graph());
    logMetric(GraphSize.class, stats::graphSize);
    logMetric(GraphCycles.class, stats::graphCycles);
    logMetric(GraphEccentricity.class, stats::graphEccentricity);
    logMetric(GraphScores.class, stats::graphScores);
    if (logMetric(GraphTopology.class, this::graphTopology)) {
      Exporter.export(graph(), id());
    }
  }

  private <T extends LoggableMetric> boolean logMetric(Class<T> type, Supplier<T> metric) {
    return LOGGER.log(LoggableMetric.KEY, type, metric);
  }

  private GraphTopology graphTopology() {
    return GraphTopology.of(id());
  }

  @Value.Lazy
  public String id() {
    return Uid.ofIntString();
  }

  @Value.Lazy
  protected Graph<Integer, DefaultEdge> graph() {
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

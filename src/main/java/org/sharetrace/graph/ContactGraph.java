package org.sharetrace.graph;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.opt.graph.fastutil.FastutilMapGraph;
import org.sharetrace.RiskPropagation;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.Loggables;
import org.sharetrace.logging.Loggers;
import org.sharetrace.logging.metrics.GraphCycleMetrics;
import org.sharetrace.logging.metrics.GraphEccentricityMetrics;
import org.sharetrace.logging.metrics.GraphScoringMetrics;
import org.sharetrace.logging.metrics.GraphSizeMetrics;
import org.sharetrace.logging.metrics.LoggableMetric;
import org.sharetrace.util.DescriptiveStats;
import org.sharetrace.util.TypedSupplier;
import org.slf4j.Logger;

/**
 * A simple graph in which a node represents a person and an edge between two nodes indicates that
 * the associated persons of the incident nodes came in contact. Nodes identifiers are zero-based
 * contiguous natural numbers. In an instance of {@link RiskPropagation}, the topology of this graph
 * is mapped to a collection {@link Node} actors.
 *
 * @see Node
 * @see Edge
 */
public class ContactGraph implements TemporalGraph {

  private static final Logger logger = Loggers.metricLogger();
  private final Loggables loggables;
  private final Graph<Integer, Edge<Integer>> graph;

  private ContactGraph(
      Graph<Integer, Edge<Integer>> graph, Set<Class<? extends Loggable>> loggable) {
    this.graph = graph;
    this.loggables = Loggables.create(loggable, logger);
    logMetrics();
  }

  private void logMetrics() {
    GraphStats<?, ?> stats = GraphStats.of(graph);
    String key = LoggableMetric.KEY;
    loggables.info(key, key, sizeMetrics(stats));
    loggables.info(key, key, cycleMetrics(stats));
    loggables.info(key, key, eccentricityMetrics(stats));
    loggables.info(key, key, scoringMetrics(stats));
  }

  private static TypedSupplier<LoggableMetric> sizeMetrics(GraphStats<?, ?> stats) {
    return TypedSupplier.of(GraphSizeMetrics.class, () -> graphSizeMetrics(stats));
  }

  private static TypedSupplier<LoggableMetric> cycleMetrics(GraphStats<?, ?> stats) {
    return TypedSupplier.of(GraphCycleMetrics.class, () -> graphCycleMetrics(stats));
  }

  private static TypedSupplier<LoggableMetric> eccentricityMetrics(GraphStats<?, ?> stats) {
    return TypedSupplier.of(GraphEccentricityMetrics.class, () -> graphEccentricityMetrics(stats));
  }

  private static TypedSupplier<LoggableMetric> scoringMetrics(GraphStats<?, ?> stats) {
    return TypedSupplier.of(GraphScoringMetrics.class, () -> graphScoringMetrics(stats));
  }

  private static GraphSizeMetrics graphSizeMetrics(GraphStats<?, ?> stats) {
    return GraphSizeMetrics.builder().nNodes(stats.nNodes()).nEdges(stats.nEdges()).build();
  }

  private static GraphCycleMetrics graphCycleMetrics(GraphStats<?, ?> stats) {
    return GraphCycleMetrics.builder().nTriangles(stats.nTriangles()).girth(stats.girth()).build();
  }

  private static GraphEccentricityMetrics graphEccentricityMetrics(GraphStats<?, ?> stats) {
    return GraphEccentricityMetrics.builder()
        .radius(stats.radius())
        .diameter(stats.diameter())
        .center(stats.center())
        .periphery(stats.periphery())
        .build();
  }

  private static GraphScoringMetrics graphScoringMetrics(GraphStats<?, ?> stats) {
    return GraphScoringMetrics.builder()
        .degeneracy(stats.degeneracy())
        .globalClusteringCoefficient(stats.globalClusteringCoefficient())
        .localClusteringCoefficient(DescriptiveStats.of(stats.localClusteringCoefficients()))
        .harmonicCentrality(DescriptiveStats.of(stats.harmonicCentralities()))
        .katzCentrality(DescriptiveStats.of(stats.katzCentralities()))
        .eigenvectorCentrality(DescriptiveStats.of(stats.eigenvectorCentralities()))
        .build();
  }

  public static ContactGraph create(
      GraphGenerator<Integer, Edge<Integer>, ?> generator,
      Set<Class<? extends Loggable>> loggable) {
    Graph<Integer, Edge<Integer>> graph = newGraph();
    generator.generateGraph(graph);
    return new ContactGraph(graph, loggable);
  }

  private static Graph<Integer, Edge<Integer>> newGraph() {
    return new FastutilMapGraph<>(new NodeIdFactory(), Edge::new, DefaultGraphType.simple());
  }

  @Override
  public IntStream nodes() {
    return graph.vertexSet().stream().mapToInt(Integer::intValue);
  }

  @Override
  public Stream<List<Integer>> edges() {
    return graph.edgeSet().stream().map(edge -> List.of(edge.source(), edge.target()));
  }

  @Override
  public long nNodes() {
    return graph.iterables().vertexCount();
  }

  @Override
  public long nEdges() {
    return graph.iterables().edgeCount();
  }

  private static final class NodeIdFactory implements Supplier<Integer> {

    private int id = 0;

    @Override
    public Integer get() {
      return id++;
    }
  }
}

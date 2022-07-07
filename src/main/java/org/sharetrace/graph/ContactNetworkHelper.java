package org.sharetrace.graph;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.AsUnmodifiableGraph;
import org.jgrapht.graph.DefaultGraphType;
import org.jgrapht.nio.GraphExporter;
import org.jgrapht.nio.graphml.GraphMLExporter;
import org.jgrapht.opt.graph.fastutil.FastutilMapGraph;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.Loggables;
import org.sharetrace.logging.Logging;
import org.sharetrace.logging.metrics.GraphCycleMetrics;
import org.sharetrace.logging.metrics.GraphEccentricityMetrics;
import org.sharetrace.logging.metrics.GraphScoringMetrics;
import org.sharetrace.logging.metrics.GraphSizeMetrics;
import org.sharetrace.logging.metrics.GraphTopologyMetric;
import org.sharetrace.logging.metrics.LoggableMetric;
import org.sharetrace.util.DescriptiveStats;
import org.sharetrace.util.TypedSupplier;
import org.slf4j.Logger;

class ContactNetworkHelper {

  private static final Logger logger = Logging.metricLogger();
  private final Graph<Integer, Edge<Integer>> contactNetwork;
  private final Loggables loggables;

  private ContactNetworkHelper(
      Graph<Integer, Edge<Integer>> contactNetwork, Set<Class<? extends Loggable>> loggable) {
    this.contactNetwork = new AsUnmodifiableGraph<>(contactNetwork);
    this.loggables = Loggables.create(loggable, logger);
  }

  public static ContactNetworkHelper create(
      GraphGenerator<Integer, Edge<Integer>, ?> generator,
      Set<Class<? extends Loggable>> loggable) {
    Graph<Integer, Edge<Integer>> contactNetwork = newContactNetwork();
    generator.generateGraph(contactNetwork);
    return new ContactNetworkHelper(contactNetwork, loggable);
  }

  private static Graph<Integer, Edge<Integer>> newContactNetwork() {
    return new FastutilMapGraph<>(userIdFactory(), Edge::new, DefaultGraphType.simple());
  }

  private static Supplier<Integer> userIdFactory() {
    int[] id = new int[] {0};
    return () -> id[0]++;
  }

  public Stream<Contact> contacts(Function<Edge<Integer>, Contact> toContact) {
    return contactNetwork.edgeSet().stream().map(toContact);
  }

  public Contact toContact(Edge<Integer> edge, Instant timestamp) {
    return Contact.builder().user1(edge.source()).user2(edge.target()).timestamp(timestamp).build();
  }

  public int nUsers() {
    return (int) contactNetwork.iterables().vertexCount();
  }

  public Graph<Integer, Edge<Integer>> contactNetwork() {
    return contactNetwork;
  }

  public int nContacts() {
    return (int) contactNetwork.iterables().edgeCount();
  }

  public IntStream users() {
    return contactNetwork.vertexSet().stream().mapToInt(Integer::intValue);
  }

  public void logMetrics() {
    GraphStats<?, ?> stats = GraphStats.of(contactNetwork);
    loggables.info(LoggableMetric.KEY, sizeMetrics(stats));
    loggables.info(LoggableMetric.KEY, cycleMetrics(stats));
    loggables.info(LoggableMetric.KEY, eccentricityMetrics(stats));
    loggables.info(LoggableMetric.KEY, scoringMetrics(stats));
    logContactNetwork();
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

  private void logContactNetwork() {
    if (loggables.loggable().contains(GraphTopologyMetric.class)) {
      String graphLabel = newGraphLabel();
      loggables.info(LoggableMetric.KEY, GraphTopologyMetric.of(graphLabel));
      try (Writer writer = newGraphWriter(graphLabel)) {
        newGraphExporter().exportGraph(contactNetwork, writer);
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
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

  private static String newGraphLabel() {
    return UUID.randomUUID().toString();
  }

  private static Writer newGraphWriter(String graphLabel) throws IOException {
    Path graphsPath = Logging.graphsLogPath();
    if (!Files.exists(graphsPath)) {
      Files.createDirectories(graphsPath);
    }
    Path filePath = Path.of(graphsPath.toString(), graphLabel + ".graphml");
    return Files.newBufferedWriter(filePath);
  }

  private static GraphExporter<Integer, Edge<Integer>> newGraphExporter() {
    GraphMLExporter<Integer, Edge<Integer>> exporter = new GraphMLExporter<>();
    exporter.setVertexIdProvider(String::valueOf);
    return exporter;
  }
}

package org.sharetrace.graph;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.AsUnmodifiableGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.GraphExporter;
import org.jgrapht.nio.graphml.GraphMLExporter;
import org.sharetrace.data.factory.ContactTimeFactory;
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

public class ContactNetworkHelper {

  private static final Logger logger = Logging.metricLogger();
  private final Graph<Integer, DefaultEdge> contactNetwork;
  private final Loggables loggables;

  private ContactNetworkHelper(
      Graph<Integer, DefaultEdge> contactNetwork, Set<Class<? extends Loggable>> loggable) {
    this.contactNetwork = new AsUnmodifiableGraph<>(contactNetwork);
    this.loggables = Loggables.create(loggable, logger);
  }

  public static ContactNetworkHelper create(
      GraphGenerator<Integer, DefaultEdge, ?> generator, Set<Class<? extends Loggable>> loggable) {
    Graph<Integer, DefaultEdge> contactNetwork = newContactNetwork();
    generator.generateGraph(contactNetwork);
    return new ContactNetworkHelper(contactNetwork, loggable);
  }

  private static Graph<Integer, DefaultEdge> newContactNetwork() {
    return GraphFactory.newUndirectedGraph();
  }

  public Stream<Contact> contacts(ContactTimeFactory contactTimeFactory) {
    return contactNetwork.edgeSet().stream().map(edge -> toContact(edge, contactTimeFactory));
  }

  private Contact toContact(DefaultEdge edge, ContactTimeFactory contactTimeFactory) {
    int user1 = contactNetwork.getEdgeSource(edge);
    int user2 = contactNetwork.getEdgeTarget(edge);
    Instant contactTime = contactTimeFactory.getContactTime(user1, user2);
    return Contact.builder().user1(user1).user2(user2).timestamp(contactTime).build();
  }

  public int nUsers() {
    return contactNetwork.vertexSet().size();
  }

  public Graph<Integer, DefaultEdge> contactNetwork() {
    return contactNetwork;
  }

  public int nContacts() {
    return contactNetwork.edgeSet().size();
  }

  public IntStream users() {
    return contactNetwork.vertexSet().stream().mapToInt(Integer::intValue);
  }

  public void logMetrics() {
    GraphStats<?, ?> stats = GraphStats.of(contactNetwork);
    loggables.log(LoggableMetric.KEY, sizeMetrics(stats));
    loggables.log(LoggableMetric.KEY, cycleMetrics(stats));
    loggables.log(LoggableMetric.KEY, eccentricityMetrics(stats));
    loggables.log(LoggableMetric.KEY, scoringMetrics(stats));
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
      loggables.log(LoggableMetric.KEY, GraphTopologyMetric.of(graphLabel));
      try (Writer writer = newGraphWriter(graphLabel)) {
        newGraphExporter().exportGraph(contactNetwork, writer);
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
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

  private static GraphExporter<Integer, DefaultEdge> newGraphExporter() {
    GraphMLExporter<Integer, DefaultEdge> exporter = new GraphMLExporter<>();
    exporter.setVertexIdProvider(String::valueOf);
    return exporter;
  }
}

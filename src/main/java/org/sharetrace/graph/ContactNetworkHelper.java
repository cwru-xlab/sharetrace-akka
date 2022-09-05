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
import org.sharetrace.logging.metrics.CycleMetrics;
import org.sharetrace.logging.metrics.EccentricityMetrics;
import org.sharetrace.logging.metrics.LoggableMetric;
import org.sharetrace.logging.metrics.ScoringMetrics;
import org.sharetrace.logging.metrics.SizeMetrics;
import org.sharetrace.logging.metrics.TopologyMetric;
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
    Graph<Integer, DefaultEdge> contactNetwork = GraphFactory.newUndirectedGraph();
    generator.generateGraph(contactNetwork);
    return new ContactNetworkHelper(contactNetwork, loggable);
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

  public int numUsers() {
    return contactNetwork.vertexSet().size();
  }

  public Graph<Integer, DefaultEdge> contactNetwork() {
    return contactNetwork;
  }

  public int numContacts() {
    return contactNetwork.edgeSet().size();
  }

  public IntStream users() {
    return contactNetwork.vertexSet().stream().mapToInt(Integer::intValue);
  }

  public void logMetrics() {
    logStats();
    logTopology();
  }

  private void logStats() {
    GraphStats<?, ?> stats = GraphStats.of(contactNetwork);
    String key = LoggableMetric.KEY;
    loggables.log(key, TypedSupplier.of(SizeMetrics.class, stats::sizeMetrics));
    loggables.log(key, TypedSupplier.of(CycleMetrics.class, stats::cycleMetrics));
    loggables.log(key, TypedSupplier.of(EccentricityMetrics.class, stats::eccentricityMetrics));
    loggables.log(key, TypedSupplier.of(ScoringMetrics.class, stats::scoringMetrics));
  }

  private void logTopology() {
    String graphLabel = UUID.randomUUID().toString();
    boolean logged = loggables.log(LoggableMetric.KEY, TopologyMetric.of(graphLabel));
    if (logged) {
      saveGraph(graphLabel);
    }
  }

  private void saveGraph(String graphLabel) {
    try (Writer writer = newGraphWriter(graphLabel)) {
      newGraphExporter().exportGraph(contactNetwork, writer);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static Writer newGraphWriter(String graphLabel) throws IOException {
    Path filePath = Path.of(graphsPath().toString(), graphLabel + ".graphml");
    return Files.newBufferedWriter(filePath);
  }

  private static GraphExporter<Integer, DefaultEdge> newGraphExporter() {
    GraphMLExporter<Integer, DefaultEdge> exporter = new GraphMLExporter<>();
    exporter.setVertexIdProvider(String::valueOf);
    return exporter;
  }

  private static Path graphsPath() throws IOException {
    Path graphsPath = Logging.graphsLogPath();
    if (!Files.exists(graphsPath)) {
      Files.createDirectories(graphsPath);
    }
    return graphsPath;
  }
}

package org.sharetrace.graph;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.graphml.GraphMLExporter;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.Logger;
import org.sharetrace.logging.Logging;
import org.sharetrace.logging.metrics.CycleMetrics;
import org.sharetrace.logging.metrics.EccentricityMetrics;
import org.sharetrace.logging.metrics.LoggableMetric;
import org.sharetrace.logging.metrics.ScoringMetrics;
import org.sharetrace.logging.metrics.SizeMetrics;
import org.sharetrace.logging.metrics.TopologyMetric;
import org.sharetrace.util.Checks;
import org.sharetrace.util.TypedSupplier;

public final class ContactNetworkHelper {

  private final Graph<Integer, DefaultEdge> contactNetwork;
  private final Logger logger;

  private ContactNetworkHelper(Graph<Integer, DefaultEdge> contactNetwork, Logger logger) {
    this.contactNetwork = contactNetwork;
    this.logger = logger;
  }

  public static ContactNetworkHelper of(
      GraphGenerator<Integer, DefaultEdge, ?> graphGenerator,
      Set<Class<? extends Loggable>> loggable) {
    Logger logger = Logging.metricsLogger(loggable); // Create early to check for null.
    Checks.isNotNull(graphGenerator, "graphGenerator");
    Graph<Integer, DefaultEdge> contactNetwork = GraphFactory.newUndirectedGraph();
    graphGenerator.generateGraph(contactNetwork);
    return new ContactNetworkHelper(contactNetwork, logger);
  }

  public Set<Contact> contacts(ContactTimeFactory contactTimeFactory) {
    Checks.isNotNull(contactTimeFactory, "contactTimeFactory");
    Set<DefaultEdge> edges = contactNetwork.edgeSet();
    return edges.stream()
        .map(edge -> toContact(edge, contactTimeFactory))
        .collect(ObjectOpenHashSet.toSetWithExpectedSize(edges.size()));
  }

  public Set<Integer> users() {
    return Collections.unmodifiableSet(contactNetwork.vertexSet());
  }

  public void logMetrics() {
    logStats();
    logTopology();
  }

  private Contact toContact(DefaultEdge edge, ContactTimeFactory factory) {
    int user1 = contactNetwork.getEdgeSource(edge);
    int user2 = contactNetwork.getEdgeTarget(edge);
    Instant time = factory.contactTime(user1, user2);
    return Contact.builder().user1(user1).user2(user2).time(time).build();
  }

  private void logStats() {
    GraphStats<?, ?> stats = GraphStats.of(contactNetwork);
    String key = LoggableMetric.KEY;
    logger.log(key, TypedSupplier.of(SizeMetrics.class, stats::sizeMetrics));
    logger.log(key, TypedSupplier.of(CycleMetrics.class, stats::cycleMetrics));
    logger.log(key, TypedSupplier.of(EccentricityMetrics.class, stats::eccentricityMetrics));
    logger.log(key, TypedSupplier.of(ScoringMetrics.class, stats::scoringMetrics));
  }

  private void logTopology() {
    String filename = UUID.randomUUID().toString();
    if (logger.log(LoggableMetric.KEY, TopologyMetric.of(filename))) {
      exportNetwork(filename);
    }
  }

  private void exportNetwork(String filename) {
    new NetworkExporter(filename).export(contactNetwork);
  }

  private static final class NetworkExporter extends GraphMLExporter<Integer, DefaultEdge>
      implements Closeable {

    private static final String FILE_EXT = ".graphml";
    private final Writer writer;

    public NetworkExporter(String filename) {
      writer = newWriter(filename);
      setVertexIdProvider(String::valueOf);
    }

    private static Writer newWriter(String filename) {
      try {
        return Files.newBufferedWriter(filePath(filename));
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    }

    private static Path filePath(String filename) throws IOException {
      Path path = Logging.graphsPath();
      if (!Files.exists(path)) {
        Files.createDirectories(path);
      }
      return Path.of(path.toString(), filename + FILE_EXT);
    }

    public void export(Graph<Integer, DefaultEdge> network) {
      exportGraph(network, writer);
    }

    @Override
    public void close() throws IOException {
      writer.close();
    }
  }
}

package org.sharetrace.graph;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.GraphExporter;
import org.jgrapht.nio.graphml.GraphMLExporter;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.Logger;
import org.sharetrace.logging.Logging;
import org.sharetrace.logging.metric.GraphCycles;
import org.sharetrace.logging.metric.GraphEccentricity;
import org.sharetrace.logging.metric.GraphScores;
import org.sharetrace.logging.metric.GraphSize;
import org.sharetrace.logging.metric.GraphTopology;
import org.sharetrace.logging.metric.LoggableMetric;
import org.sharetrace.util.TypedSupplier;

final class ContactNetworkHelper {

  private final Graph<Integer, DefaultEdge> contactNetwork;
  private final Logger logger;

  private ContactNetworkHelper(Graph<Integer, DefaultEdge> contactNetwork, Logger logger) {
    this.contactNetwork = contactNetwork;
    this.logger = logger;
  }

  public static ContactNetworkHelper of(
      GraphGenerator<Integer, DefaultEdge, ?> graphGenerator,
      Set<Class<? extends Loggable>> loggable) {
    Graph<Integer, DefaultEdge> contactNetwork = GraphFactory.newUndirectedGraph();
    graphGenerator.generateGraph(contactNetwork);
    return new ContactNetworkHelper(contactNetwork, Logging.metricsLogger(loggable));
  }

  private static TypedSupplier<LoggableMetric> graphTopology(String filename) {
    return TypedSupplier.of(GraphTopology.class, () -> GraphTopology.of(filename));
  }

  public Set<Contact> contacts(ContactTimeFactory timeFactory) {
    Set<Contact> contacts = new ObjectOpenHashSet<>(contactNetwork.edgeSet().size());
    contactNetwork.edgeSet().forEach(edge -> contacts.add(contactOf(edge, timeFactory)));
    return Collections.unmodifiableSet(contacts);
  }

  public Set<Integer> users() {
    return Collections.unmodifiableSet(contactNetwork.vertexSet());
  }

  public void logMetrics() {
    GraphStats<?, ?> stats = GraphStats.of(contactNetwork);
    String key = LoggableMetric.KEY;
    logger.log(key, TypedSupplier.of(GraphSize.class, stats::graphSize));
    logger.log(key, TypedSupplier.of(GraphCycles.class, stats::graphCycles));
    logger.log(key, TypedSupplier.of(GraphEccentricity.class, stats::graphEccentricity));
    logger.log(key, TypedSupplier.of(GraphScores.class, stats::graphScores));
    String filename = UUID.randomUUID().toString();
    if (logger.log(key, graphTopology(filename))) {
      exportNetwork(filename);
    }
  }

  private Contact contactOf(DefaultEdge edge, ContactTimeFactory factory) {
    int user1 = contactNetwork.getEdgeSource(edge);
    int user2 = contactNetwork.getEdgeTarget(edge);
    Instant time = factory.contactTime(user1, user2);
    return Contact.builder().user1(user1).user2(user2).time(time).build();
  }

  private void exportNetwork(String filename) {
    new NetworkExporter<Integer, DefaultEdge>(filename).export(contactNetwork);
  }

  private static final class NetworkExporter<V, E> {

    private static final String FILE_EXT = ".graphml";
    private final GraphExporter<V, E> exporter;
    private final File file;

    public NetworkExporter(String filename) {
      file = newFile(filename);
      exporter = new GraphMLExporter<>(String::valueOf);
    }

    private static File newFile(String filename) {
      String directory = ensureExists(Logging.graphsPath()).toString();
      return Path.of(directory, filename + FILE_EXT).toFile();
    }

    private static Path ensureExists(Path path) {
      if (Files.notExists(path)) {
        try {
          Files.createDirectories(path);
        } catch (IOException exception) {
          throw new UncheckedIOException(exception);
        }
      }
      return path;
    }

    public void export(Graph<V, E> network) {
      exporter.exportGraph(network, file);
    }
  }
}

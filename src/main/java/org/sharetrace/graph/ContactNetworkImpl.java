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

final class ContactNetworkImpl implements ContactNetwork {

  private final Graph<Integer, DefaultEdge> graph;
  private final ContactTimeFactory timeFactory;
  private final Logger logger;

  private ContactNetworkImpl(
      Graph<Integer, DefaultEdge> graph, ContactTimeFactory timeFactory, Logger logger) {
    this.graph = graph;
    this.timeFactory = timeFactory;
    this.logger = logger;
  }

  public static ContactNetworkImpl of(
      GraphGenerator<Integer, DefaultEdge, ?> graphGenerator,
      ContactTimeFactory timeFactory,
      Set<Class<? extends Loggable>> loggable) {
    Graph<Integer, DefaultEdge> graph = GraphFactory.newUndirectedGraph();
    graphGenerator.generateGraph(graph);
    return new ContactNetworkImpl(graph, timeFactory, Logging.metricsLogger(loggable));
  }

  private static TypedSupplier<LoggableMetric> graphTopology(String filename) {
    return TypedSupplier.of(GraphTopology.class, () -> GraphTopology.of(filename));
  }

  @Override
  public Set<Integer> users() {
    return Collections.unmodifiableSet(graph.vertexSet());
  }

  @Override
  public Set<Contact> contacts() {
    Set<Contact> contacts = new ObjectOpenHashSet<>(graph.edgeSet().size());
    graph.edgeSet().forEach(edge -> contacts.add(contactFrom(edge)));
    return Collections.unmodifiableSet(contacts);
  }

  @Override
  public void logMetrics() {
    GraphStats<?, ?> stats = GraphStats.of(graph);
    String key = LoggableMetric.KEY;
    logger.log(key, TypedSupplier.of(GraphSize.class, stats::graphSize));
    logger.log(key, TypedSupplier.of(GraphCycles.class, stats::graphCycles));
    logger.log(key, TypedSupplier.of(GraphEccentricity.class, stats::graphEccentricity));
    logger.log(key, TypedSupplier.of(GraphScores.class, stats::graphScores));
    String filename = UUID.randomUUID().toString();
    if (logger.log(key, graphTopology(filename))) {
      exportGraph(filename);
    }
  }

  private Contact contactFrom(DefaultEdge edge) {
    int user1 = graph.getEdgeSource(edge);
    int user2 = graph.getEdgeTarget(edge);
    Instant contactTime = timeFactory.contactTime(user1, user2);
    return Contact.builder().user1(user1).user2(user2).time(contactTime).build();
  }

  private void exportGraph(String filename) {
    new Exporter<Integer, DefaultEdge>(filename).export(graph);
  }

  private static final class Exporter<V, E> {

    private static final String FILE_EXT = ".graphml";
    private final GraphExporter<V, E> exporter;
    private final File file;

    public Exporter(String filename) {
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

    public void export(Graph<V, E> graph) {
      exporter.exportGraph(graph, file);
    }
  }
}

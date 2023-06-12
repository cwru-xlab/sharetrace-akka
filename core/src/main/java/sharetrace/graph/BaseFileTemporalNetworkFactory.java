package sharetrace.graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.model.TimestampReference;
import sharetrace.util.IdFactory;

@Value.Immutable
abstract class BaseFileTemporalNetworkFactory<V> extends AbstractTemporalNetworkFactory<V>
    implements TimestampReference {

  private static final String TEST_INT = "1";

  public abstract Path path();

  public abstract String delimiter();

  public abstract Function<String, V> nodeParser();

  @Value.Default
  public Function<String, Instant> timestampParser() {
    return timestamp -> Instant.ofEpochSecond(Long.parseLong(timestamp.strip()));
  }

  @Value.Default
  public BinaryOperator<Instant> mergeStrategy() {
    return BinaryOperator.maxBy(Instant::compareTo);
  }

  @Override
  protected Graph<V, TemporalEdge> newTarget() {
    return GraphFactory.newGraph(nodeParser().apply(TEST_INT));
  }

  @Override
  protected TemporalNetwork<V> newNetwork(Graph<V, TemporalEdge> target) {
    String type = path().getFileName().toString();
    return new SimpleTemporalNetwork<>(target, IdFactory.newIntString(), type);
  }

  @Override
  protected GraphGenerator<V, TemporalEdge, V> graphGenerator() {
    return this::generateGraph;
  }

  private void generateGraph(Graph<V, TemporalEdge> target, Map<String, V> resultMap) {
    Instant extremum = parseNetwork(target);
    adjustTimestamps(target, extremum);
  }

  private Instant parseNetwork(Graph<V, TemporalEdge> target) {
    try (BufferedReader reader = Files.newBufferedReader(path())) {
      Iterable<String> edges = reader.lines()::iterator;
      Instant extremum = Instant.MIN;
      for (String edge : edges) {
        String[] args = edge.split(delimiter());
        V v1 = nodeParser().apply(args[1]);
        V v2 = nodeParser().apply(args[2]);
        if (!v1.equals(v2)) {
          Instant timestamp = timestampParser().apply(args[2]);
          mergeWithNodes(target, v1, v2, timestamp);
          extremum = mergeStrategy().apply(extremum, timestamp);
        }
      }
      return extremum;
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private void mergeWithNodes(Graph<V, TemporalEdge> target, V v1, V v2, Instant timestamp) {
    target.addVertex(v1);
    target.addVertex(v2);
    TemporalEdge edge = target.getEdge(v1, v2);
    if (edge != null) {
      edge.mergeTimestamp(timestamp, mergeStrategy());
    } else {
      target.addEdge(v1, v2).setTimestamp(timestamp);
    }
  }

  private void adjustTimestamps(Graph<V, TemporalEdge> target, Instant extremum) {
    Duration offset = Duration.between(extremum, referenceTimestamp());
    target.edgeSet().forEach(edge -> edge.mapTimestamp(t -> t.plus(offset)));
  }
}

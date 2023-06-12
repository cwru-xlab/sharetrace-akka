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
import sharetrace.util.Identifiers;

@Value.Immutable
abstract class BaseFileTemporalNetworkFactory<V> extends AbstractTemporalNetworkFactory<V>
    implements TimestampReference {

  public abstract Path path();

  public abstract String delimiter();

  public abstract Function<String, V> vertexParser();

  @Override
  protected Graph<V, TemporalEdge> newTarget() {
    return TemporalNetworkFactoryHelper.newTarget();
  }

  @Override
  protected TemporalNetwork<V> newNetwork(Graph<V, TemporalEdge> target) {
    return new SimpleTemporalNetwork<>(
        target, Identifiers.newIntString(), path().getFileName().toString());
  }

  @Value.Default
  public Function<String, Instant> timestampParser() {
    return timestamp -> Instant.ofEpochSecond(Long.parseLong(timestamp.strip()));
  }

  @Value.Default
  public BinaryOperator<Instant> mergeStrategy() {
    return BinaryOperator.maxBy(Instant::compareTo);
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
        V v1 = vertexParser().apply(args[1]);
        V v2 = vertexParser().apply(args[2]);
        if (!v1.equals(v2)) {
          Instant timestamp = timestampParser().apply(args[2]);
          mergeEdgeWithVertices(target, v1, v2, timestamp);
          extremum = mergeStrategy().apply(extremum, timestamp);
        }
      }
      return extremum;
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private void mergeEdgeWithVertices(Graph<V, TemporalEdge> target, V v1, V v2, Instant timestamp) {
    target.addVertex(v1);
    target.addVertex(v2);
    TemporalEdge edge = target.getEdge(v1, v2);
    if (edge != null) {
      Instant newTimestamp = mergeStrategy().apply(edge.getTimestamp(), timestamp);
      edge.setTimestamp(newTimestamp);
    } else {
      target.addEdge(v1, v2).setTimestamp(timestamp);
    }
  }

  private void adjustTimestamps(Graph<V, TemporalEdge> target, Instant extremum) {
    Duration offset = Duration.between(extremum, referenceTimestamp());
    for (TemporalEdge edge : target.edgeSet()) {
      Instant newTimestamp = edge.getTimestamp().plus(offset);
      edge.setTimestamp(newTimestamp);
    }
  }
}

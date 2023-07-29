package sharetrace.graph;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.Timestamped;

@Buildable
public record FileContactNetworkFactory<V>(
    Path path, String delimiter, Function<String, V> nodeParser, Instant timestamp)
    implements ContactNetworkFactory<V>, Timestamped {

  private static final String TEST_INT = "1";
  private static final BinaryOperator<Instant> MERGER = BinaryOperator.maxBy(Instant::compareTo);

  @Override
  public Graph<V, TemporalEdge> newTarget() {
    return GraphFactory.newGraph(nodeParser.apply(TEST_INT));
  }

  @Override
  public ContactNetwork<V> newContactNetwork(Graph<V, TemporalEdge> target) {
    return new SimpleContactNetwork<>(target, path.getFileName().toString());
  }

  @Override
  public GraphGenerator<V, TemporalEdge, V> graphGenerator() {
    return this::generateGraph;
  }

  private void generateGraph(Graph<V, TemporalEdge> target, Map<String, V> resultMap) {
    var maxTimestamp = parseNetwork(target);
    adjustTimestamps(target, maxTimestamp);
  }

  private Instant parseNetwork(Graph<V, TemporalEdge> target) {
    try (var reader = Files.newBufferedReader(path)) {
      Iterable<String> edges = reader.lines()::iterator;
      var maxTimestamp = Instant.MIN;
      for (var edge : edges) {
        var args = edge.split(delimiter);
        var v1 = nodeParser.apply(args[1]);
        var v2 = nodeParser.apply(args[2]);
        if (!v1.equals(v2)) {
          var timestamp = parseTimestamp(args[2]);
          mergeWithNodes(target, v1, v2, timestamp);
          maxTimestamp = MERGER.apply(maxTimestamp, timestamp);
        }
      }
      return maxTimestamp;
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private Instant parseTimestamp(String timestamp) {
    return Instant.ofEpochSecond(Long.parseLong(timestamp.strip()));
  }

  private void mergeWithNodes(Graph<V, TemporalEdge> target, V v1, V v2, Instant timestamp) {
    target.addVertex(v1);
    target.addVertex(v2);
    var edge = target.getEdge(v1, v2);
    if (edge != null) {
      edge.mergeTimestamp(timestamp, MERGER);
    } else {
      target.addEdge(v1, v2).setTimestamp(timestamp);
    }
  }

  private void adjustTimestamps(Graph<V, TemporalEdge> target, Instant maxTimestamp) {
    var offset = Duration.between(maxTimestamp, timestamp);
    for (var edge : target.edgeSet()) {
      edge.mapTimestamp(t -> t.plus(offset));
      target.setEdgeWeight(edge, edge.weight());
    }
  }
}

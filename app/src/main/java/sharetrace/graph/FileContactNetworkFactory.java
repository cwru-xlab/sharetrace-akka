package sharetrace.graph;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.Timestamped;
import sharetrace.util.Instants;

@Buildable
public record FileContactNetworkFactory(Path path, String delimiter, Instant timestamp)
    implements ContactNetworkFactory, Timestamped {

  @Override
  public ContactNetwork newContactNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleContactNetwork(target, path.getFileName().toString());
  }

  @Override
  public GraphGenerator<Integer, TemporalEdge, ?> graphGenerator() {
    return this::generateGraph;
  }

  private void generateGraph(Graph<Integer, TemporalEdge> target, Map<String, ?> resultMap) {
    var max = parseNetwork(target);
    adjustTimestamps(target, max);
  }

  private Instant parseNetwork(Graph<Integer, TemporalEdge> target) {
    var max = new AtomicReference<>(Instant.EPOCH);
    try (var edges = Files.lines(path)) {
      edges.forEach(edge -> processEdge(edge, target, max));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    return max.get();
  }

  private void processEdge(
      String edge, Graph<Integer, TemporalEdge> target, AtomicReference<Instant> max) {
    var args = edge.split(delimiter);
    var v1 = Integer.parseInt(args[1]);
    var v2 = Integer.parseInt(args[2]);
    if (v1 != v2) {
      var timestamp = parseTimestamp(args[0]);
      Graphs.addTemporalEdge(target, v1, v2, timestamp);
      max.set(Instants.max(max.get(), timestamp));
    }
  }

  private Instant parseTimestamp(String timestamp) {
    return Instant.ofEpochSecond(Long.parseLong(timestamp.strip()));
  }

  private void adjustTimestamps(Graph<Integer, TemporalEdge> target, Instant max) {
    var offset = Duration.between(max, timestamp);
    for (var edge : target.edgeSet()) {
      edge.updateTime(t -> t.plus(offset));
    }
  }
}

package sharetrace.graph;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.Timestamp;

@Buildable
public record FileContactNetworkFactory(Path path, String delimiter, Timestamp referenceTime)
    implements ContactNetworkFactory {

  @Override
  public ContactNetwork newContactNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleContactNetwork(target, path.getFileName().toString());
  }

  @Override
  public GraphGenerator<Integer, TemporalEdge, ?> graphGenerator() {
    return this::generateGraph;
  }

  private void generateGraph(Graph<Integer, TemporalEdge> target, Map<String, ?> resultMap) {
    var max = new AtomicReference<>(Timestamp.MIN);
    try (var edges = Files.lines(path)) {
      edges.forEach(edge -> processEdge(edge, target, max));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    var offset = Duration.between(max.get(), referenceTime);
    target.edgeSet().forEach(edge -> edge.updateTime(t -> t.plus(offset)));
  }

  private void processEdge(
      String edge, Graph<Integer, TemporalEdge> target, AtomicReference<Timestamp> max) {
    var args = edge.split(delimiter);
    var v1 = Integer.parseInt(args[1]);
    var v2 = Integer.parseInt(args[2]);
    if (v1 != v2) {
      var contactTime = Timestamp.ofEpochSeconds(Long.parseLong(args[0]));
      Graphs.addTemporalEdge(target, v1, v2, contactTime);
      max.updateAndGet(t -> Timestamp.max(t, contactTime));
    }
  }
}

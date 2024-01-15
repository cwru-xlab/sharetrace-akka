package sharetrace.graph;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.Buildable;

@Buildable
public record FileContactNetworkFactory(Path path, String delimiter, long referenceTime)
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
    var max = new AtomicLong();
    try (var edges = Files.lines(path)) {
      edges.forEach(edge -> processEdge(edge, target, max));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
    var offset = Math.subtractExact(referenceTime, max.get());
    target.edgeSet().forEach(edge -> edge.updateTime(t -> Math.addExact(t, offset)));
  }

  private void processEdge(String edge, Graph<Integer, TemporalEdge> target, AtomicLong max) {
    var args = edge.split(delimiter);
    var v1 = Integer.parseInt(args[1]);
    var v2 = Integer.parseInt(args[2]);
    if (v1 != v2) {
      var contactTime = Long.parseLong(args[0]) * 1000L;
      Graphs.addTemporalEdge(target, v1, v2, contactTime);
      max.updateAndGet(t -> Math.max(t, contactTime));
    }
  }
}

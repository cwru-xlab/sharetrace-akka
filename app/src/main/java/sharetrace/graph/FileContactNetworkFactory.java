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
import sharetrace.model.factory.ContactNetworkFactory;
import sharetrace.model.graph.Graphs;
import sharetrace.model.graph.TemporalEdge;

@Buildable
public record FileContactNetworkFactory(Path path, String delimiter, long referenceTime)
    implements ContactNetworkFactory {

  @Override
  public String type() {
    return "File";
  }

  @Override
  public GraphGenerator<Integer, TemporalEdge, ?> graphGenerator() {
    return this::generateGraph;
  }

  private void generateGraph(Graph<Integer, TemporalEdge> target, Map<String, ?> resultMap) {
    var maxContactTime = new AtomicLong();
    try (var edges = Files.lines(path)) {
      edges.forEach(edge -> processEdge(edge, target, maxContactTime));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    // Newest contact time = reference time.
    var offset = Math.subtractExact(referenceTime, maxContactTime.get());
    target.edgeSet().forEach(edge -> edge.updateTime(t -> Math.addExact(t, offset)));
  }

  private void processEdge(
      String edge, Graph<Integer, TemporalEdge> target, AtomicLong maxContactTime) {
    var args = edge.split(delimiter);
    var v1 = Integer.parseInt(args[1]);
    var v2 = Integer.parseInt(args[2]);
    if (v1 != v2) {
      // Assumes the contact times are stored in seconds.
      var contactTime = Long.parseLong(args[0]) * 1000;
      Graphs.addTemporalEdge(target, v1, v2, contactTime);
      maxContactTime.updateAndGet(t -> Math.max(t, contactTime));
    }
  }
}

package sharetrace.graph;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jgrapht.Graph;
import org.jgrapht.nio.GraphExporter;
import org.jgrapht.nio.graphml.GraphMLExporter;
import sharetrace.Buildable;

@Buildable
public record TemporalGraphExporter(Path directory, String filename) {

  public void export(Graph<Integer, TemporalEdge> graph) {
    exporter().exportGraph(graph, file(directory, filename));
  }

  private GraphExporter<Integer, TemporalEdge> exporter() {
    var exporter = new GraphMLExporter<Integer, TemporalEdge>(String::valueOf);
    exporter.setExportEdgeWeights(true);
    return exporter;
  }

  private File file(Path directory, String filename) {
    return ensureExists(directory).resolve(filename + ".graphml").toFile();
  }

  private Path ensureExists(Path path) {
    if (Files.notExists(path)) {
      try {
        return Files.createDirectories(path);
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    }
    return path;
  }
}

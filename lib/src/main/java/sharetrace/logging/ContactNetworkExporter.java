package sharetrace.logging;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jgrapht.nio.GraphExporter;
import org.jgrapht.nio.graphml.GraphMLExporter;
import sharetrace.Buildable;
import sharetrace.model.graph.ContactNetwork;
import sharetrace.model.graph.TemporalEdge;

@SuppressWarnings("unused")
@Buildable
public record ContactNetworkExporter(Path directory, String filename) {

  public void export(ContactNetwork network) {
    exporter().exportGraph(network, file(directory, filename));
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

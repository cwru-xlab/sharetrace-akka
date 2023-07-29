package sharetrace.graph;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jgrapht.nio.GraphExporter;
import org.jgrapht.nio.graphml.GraphMLExporter;

public final class TemporalNetworkExporter<V> {

  private final GraphExporter<V, TemporalEdge> delegate;
  private final File file;

  public TemporalNetworkExporter(Path directory, String filename) {
    delegate = newExporter();
    file = newFile(directory, filename);
  }

  public static <V> void export(ContactNetwork<V> network, Path directory, Object filename) {
    new TemporalNetworkExporter<V>(directory, String.valueOf(filename)).export(network);
  }

  public void export(ContactNetwork<V> network) {
    delegate.exportGraph(network, file);
  }

  private GraphExporter<V, TemporalEdge> newExporter() {
    var exporter = new GraphMLExporter<V, TemporalEdge>(String::valueOf);
    exporter.setExportEdgeWeights(true);
    return exporter;
  }

  private File newFile(Path directory, String filename) {
    return Path.of(ensureExists(directory).toString(), filename + ".graphml").toFile();
  }

  private Path ensureExists(Path path) {
    if (Files.notExists(path)) {
      try {
        Files.createDirectories(path);
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    }
    return path;
  }
}

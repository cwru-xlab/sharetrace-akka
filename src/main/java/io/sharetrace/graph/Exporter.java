package io.sharetrace.graph;

import io.sharetrace.util.logging.Logging;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jgrapht.Graph;
import org.jgrapht.nio.GraphExporter;
import org.jgrapht.nio.graphml.GraphMLExporter;

final class Exporter<V, E> {

  private final GraphExporter<V, E> exporter;
  private final File file;

  public Exporter(String filename) {
    file = newFile(filename);
    exporter = new GraphMLExporter<>(String::valueOf);
  }

  public static <V, E> void export(Graph<V, E> graph, String filename) {
    new Exporter<V, E>(filename).export(graph);
  }

  private static File newFile(String filename) {
    String directory = ensureExists(Logging.graphsPath()).toString();
    return Path.of(directory, filename + ".graphml").toFile();
  }

  private static Path ensureExists(Path path) {
    if (Files.notExists(path)) {
      try {
        Files.createDirectories(path);
      } catch (IOException exception) {
        throw new UncheckedIOException(exception);
      }
    }
    return path;
  }

  public void export(Graph<V, E> graph) {
    exporter.exportGraph(graph, file);
  }
}

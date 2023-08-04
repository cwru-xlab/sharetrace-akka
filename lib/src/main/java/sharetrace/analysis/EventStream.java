package sharetrace.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public final class EventStream {

  private EventStream() {}

  @SuppressWarnings("resource")
  public static Stream<String> of(Path directory) throws IOException {
    return Files.list(directory).filter(EventStream::isEventLog).flatMap(EventStream::stream);
  }

  private static boolean isEventLog(Path path) {
    return filename(path).startsWith("event") && (isCompressed(path) || isUncompressed(path));
  }

  private static Stream<String> stream(Path file) {
    var buffer = 8192;
    try {
      var input = Files.newInputStream(file);
      if (isCompressed(file)) {
        input = new GZIPInputStream(input, buffer);
      }
      return new BufferedReader(new InputStreamReader(input), buffer).lines();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private static boolean isCompressed(Path file) {
    return filename(file).endsWith(".gz");
  }

  private static boolean isUncompressed(Path file) {
    return filename(file).endsWith(".log");
  }

  private static String filename(Path path) {
    return path.getFileName().toString();
  }
}

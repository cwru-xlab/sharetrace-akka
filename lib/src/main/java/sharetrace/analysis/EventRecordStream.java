package sharetrace.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import sharetrace.analysis.model.EventRecord;
import sharetrace.util.Parser;

public record EventRecordStream(Parser<String, EventRecord> parser) {

  @SuppressWarnings("resource")
  public Stream<EventRecord> open(Path directory) throws IOException {
    return Files.list(directory)
        .filter(this::isEventLog)
        .sorted(this::compare)
        .flatMap(this::records)
        .map(parser::parse);
  }

  private boolean isEventLog(Path path) {
    return filename(path).startsWith("event") && (isCompressed(path) || isUncompressed(path));
  }

  private int compare(Path left, Path right) {
    /* Compressed event logs include events that occurred before events in non-compressed logs, so
    they should be read first. If both event logs are (non-)compressed, then compare normally. */
    if (isCompressed(left) ^ isCompressed(right)) {
      return isCompressed(left) ? -1 : 1;
    } else {
      return left.compareTo(right);
    }
  }

  private Stream<String> records(Path path) {
    var buffer = 8192; // Default buffer size used by BufferedReader
    try {
      var input = Files.newInputStream(path);
      // Buffering is required here as well for performant throughput.
      if (isCompressed(path)) {
        input = new GZIPInputStream(input, buffer);
      }
      return new BufferedReader(new InputStreamReader(input), buffer).lines();
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private boolean isCompressed(Path path) {
    return filename(path).endsWith(".gz");
  }

  private boolean isUncompressed(Path path) {
    return filename(path).endsWith(".log");
  }

  private String filename(Path path) {
    return path.getFileName().toString();
  }
}

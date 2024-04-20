package sharetrace.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import sharetrace.analysis.model.EventRecord;

public record EventRecordsLoader(ObjectMapper mapper) {

  @SuppressWarnings("resource")
  public Stream<EventRecord> loadEventRecords(Path directory) throws IOException {
    return Files.list(directory)
        .filter(this::isEventLog)
        .sorted(this::compare)
        .flatMap(this::lines)
        .map(new EventRecordParser(mapper)::parse);
  }

  private boolean isEventLog(Path path) {
    return getFilename(path).startsWith("event") && (isCompressed(path) || isUncompressed(path));
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

  private Stream<String> lines(Path path) {
    var buffer = 8192; // Default buffer size used by BufferedReader
    try {
      var input = Files.newInputStream(path);
      // Buffering is required here as well for performant throughput.
      if (isCompressed(path)) {
        input = new GZIPInputStream(input, buffer);
      }
      return new BufferedReader(new InputStreamReader(input), buffer).lines();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private boolean isCompressed(Path path) {
    return getFilename(path).endsWith(".gz");
  }

  private boolean isUncompressed(Path path) {
    return getFilename(path).endsWith(".log");
  }

  private String getFilename(Path path) {
    return path.getFileName().toString();
  }

  private record EventRecordParser(ObjectReader reader) {

    public EventRecordParser(ObjectMapper mapper) {
      this(mapper.readerFor(EventRecord.class));
    }

    public EventRecord parse(String input) {
      try {
        return reader.readValue(input);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }
}

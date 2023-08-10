package sharetrace.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import sharetrace.analysis.model.EventRecord;
import sharetrace.util.Parser;

public record EventRecordParser(ObjectReader reader) implements Parser<String, EventRecord> {

  public EventRecordParser(ObjectMapper mapper) {
    this(mapper.readerFor(EventRecord.class));
  }

  @Override
  public EventRecord parse(String input) {
    try {
      return reader.readValue(input);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}

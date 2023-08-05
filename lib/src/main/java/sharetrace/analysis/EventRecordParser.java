package sharetrace.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import sharetrace.analysis.model.EventRecord;
import sharetrace.util.Parser;

public record EventRecordParser(ObjectMapper mapper) implements Parser<String, EventRecord> {

  @Override
  public EventRecord parse(String input) {
    try {
      return mapper.readerFor(EventRecord.class).readValue(input);
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}

package sharetrace.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import sharetrace.analysis.model.EventRecord;
import sharetrace.logging.LogRecord;
import sharetrace.logging.event.Event;
import sharetrace.util.Parser;

public record EventRecordParser(ObjectMapper mapper) implements Parser<String, EventRecord> {

  @Override
  public EventRecord parse(String input) {
    try {
      var record = getRecord(input);
      return new EventRecord(getKey(record), getEvent(record));
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }

  private JsonNode getRecord(String input) throws IOException {
    return mapper.readTree(input);
  }

  private String getKey(JsonNode record) {
    return record.get(LogRecord.key()).asText();
  }

  private Event getEvent(JsonNode record) throws IOException {
    return mapper.readValue(record.get(Event.key()).traverse(), Event.class);
  }
}

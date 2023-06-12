package sharetrace.util.logging.event;

import java.time.Instant;
import sharetrace.util.logging.LogRecord;

public interface EventRecord extends LogRecord {

  String KEY = "event";

  String self();

  Instant timestamp();
}

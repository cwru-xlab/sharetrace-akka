package sharetrace.util.logging.event;

import sharetrace.model.Timestamped;
import sharetrace.util.logging.LogRecord;

public interface EventRecord extends LogRecord, Timestamped {

  String KEY = "event";

  String self();
}

package sharetrace.logging.event;

import sharetrace.logging.LogRecord;
import sharetrace.model.Timestamped;

public interface Event extends LogRecord, Timestamped {

  static String key() {
    return "event";
  }
}

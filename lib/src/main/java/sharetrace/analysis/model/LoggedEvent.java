package sharetrace.analysis.model;

import sharetrace.logging.event.Event;
import sharetrace.model.Timestamp;
import sharetrace.model.Timestamped;

public record LoggedEvent(String type, Timestamp timestamp) implements Timestamped {

  public static LoggedEvent from(Event event) {
    return new LoggedEvent(event.getClass().getSimpleName(), event.timestamp());
  }
}

package sharetrace.analysis.model;

import java.time.Instant;
import sharetrace.logging.event.Event;
import sharetrace.model.Timestamped;

public record LoggedEvent(String type, Instant timestamp) implements Timestamped {

  public static LoggedEvent from(Event event) {
    return new LoggedEvent(event.getClass().getSimpleName(), event.timestamp());
  }
}

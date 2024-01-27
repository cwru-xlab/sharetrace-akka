package sharetrace.analysis.model;

import sharetrace.logging.event.Event;

public record LoggedEvent(Class<? extends Event> type, long timestamp) implements Event {

  public static LoggedEvent from(Event event) {
    return new LoggedEvent(event.getClass(), event.timestamp());
  }
}

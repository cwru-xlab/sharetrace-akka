package sharetrace.analysis.model;

import sharetrace.logging.event.Event;

public record TypedEvent(Class<? extends Event> type, long timestamp) implements Event {}

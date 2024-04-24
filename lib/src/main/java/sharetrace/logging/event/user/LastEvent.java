package sharetrace.logging.event.user;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LastEvent(int self, @JsonProperty("t") long timestamp) implements UserEvent {}

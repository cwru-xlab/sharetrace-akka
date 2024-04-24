package sharetrace.logging.event.user;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ContactEvent(
    int self, @JsonProperty("c") int contact, @JsonProperty("t") long contactTime)
    implements UserEvent {}

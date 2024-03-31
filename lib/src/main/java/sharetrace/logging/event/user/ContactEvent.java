package sharetrace.logging.event.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("C")
public record ContactEvent(
    int self, @JsonProperty("c") int contact, @JsonProperty("ct") long contactTime, long timestamp)
    implements UserEvent {}

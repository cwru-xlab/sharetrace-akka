package sharetrace.logging.event.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import sharetrace.model.message.RiskScoreMessage;

public record ReceiveEvent(
    int self,
    @JsonProperty("c") int contact,
    @JsonProperty("m") RiskScoreMessage message,
    long timestamp)
    implements UserEvent {}

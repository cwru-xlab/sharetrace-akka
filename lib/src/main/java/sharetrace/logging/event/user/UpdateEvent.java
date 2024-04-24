package sharetrace.logging.event.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import sharetrace.model.message.RiskScoreMessage;

public record UpdateEvent(
    int self,
    @JsonProperty("p") RiskScoreMessage previous,
    @JsonProperty("c") RiskScoreMessage current)
    implements UserEvent {}

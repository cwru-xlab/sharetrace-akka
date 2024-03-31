package sharetrace.logging.event.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import sharetrace.model.message.RiskScoreMessage;

@JsonTypeName("U")
public record UpdateEvent(
    int self,
    @JsonProperty("p") RiskScoreMessage previous,
    @JsonProperty("c") RiskScoreMessage current,
    long timestamp)
    implements UserEvent {}

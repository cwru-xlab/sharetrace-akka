package sharetrace.model.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;

public record RiskScoreMessage(@JsonProperty("s") RiskScore score, @JsonIgnore int sender, @JsonProperty("o") int origin)
    implements TemporalScore, UserMessage {

  public static RiskScoreMessage ofOrigin(RiskScore score, int origin) {
    return new RiskScoreMessage(score, origin, origin);
  }

  @Override
  @JsonIgnore
  public double value() {
    return score.value();
  }

  @Override
  @JsonIgnore
  public long timestamp() {
    return score.timestamp();
  }

  @Override
  public long expiryTime() {
    return score.expiryTime();
  }
}

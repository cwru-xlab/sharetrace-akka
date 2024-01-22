package sharetrace.model.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;

public record RiskScoreMessage(RiskScore score, @JsonIgnore int sender, int origin)
    implements TemporalScore, UserMessage {

  @Override
  public double value() {
    return score.value();
  }

  @Override
  public long timestamp() {
    return score.timestamp();
  }

  @Override
  public long expiryTime() {
    return score.expiryTime();
  }
}

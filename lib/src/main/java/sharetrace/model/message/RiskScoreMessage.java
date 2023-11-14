package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.model.Timestamp;

public record RiskScoreMessage(@JsonIgnore ActorRef<UserMessage> sender, RiskScore score, int id)
    implements TemporalScore, UserMessage {

  @Override
  public double value() {
    return score.value();
  }

  @Override
  public Timestamp timestamp() {
    return score.timestamp();
  }

  @Override
  public Timestamp expiryTime() {
    return score.expiryTime();
  }
}

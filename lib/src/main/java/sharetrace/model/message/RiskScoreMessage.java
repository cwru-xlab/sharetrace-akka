package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;

public record RiskScoreMessage(@JsonIgnore ActorRef<UserMessage> sender, RiskScore score, int id)
    implements TemporalScore, UserMessage {

  @Override
  public double value() {
    return score.value();
  }

  @Override
  public Instant timestamp() {
    return score.timestamp();
  }

  @Override
  public Instant expiresAt() {
    return score.expiresAt();
  }
}

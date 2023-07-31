package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import sharetrace.model.Identifiable;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.util.IdFactory;

public record RiskScoreMessage(@JsonIgnore ActorRef<UserMessage> sender, RiskScore score, String id)
    implements TemporalScore, Identifiable, UserMessage {

  public RiskScoreMessage(ActorRef<UserMessage> sender, RiskScore score) {
    this(sender, score, IdFactory.nextUlid());
  }

  @Override
  public float value() {
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

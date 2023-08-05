package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.util.IdFactory;

public record RiskScoreMessage(ActorRef<UserMessage> sender, RiskScore score, long id)
    implements TemporalScore, UserMessage {

  public RiskScoreMessage(ActorRef<UserMessage> sender, RiskScore score) {
    this(sender, score, IdFactory.newLong());
  }

  @Override
  @JsonIgnore
  public ActorRef<UserMessage> sender() {
    return sender;
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

package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;

public record RiskScoreMessage(ActorRef<UserMessage> sender, RiskScore score, long id)
    implements TemporalScore, UserMessage {

  public RiskScoreMessage(ActorRef<UserMessage> sender, RiskScore score) {
    this(sender, score, ThreadLocalRandom.current().nextLong(Long.MAX_VALUE));
  }

  @Override
  @JsonIgnore
  public ActorRef<UserMessage> sender() {
    return sender;
  }

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

package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Duration;
import java.time.Instant;
import org.immutables.value.Value;
import sharetrace.model.Identifiable;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.util.IdFactory;

@Value.Immutable
abstract class BaseRiskScoreMessage implements UserMessage, Identifiable, TemporalScore {

  @JsonIgnore
  @Value.Parameter
  public abstract ActorRef<UserMessage> sender();

  @JsonUnwrapped
  @Value.Parameter
  public abstract RiskScore score();

  @Override
  @Value.Default
  public long id() {
    return IdFactory.newLong();
  }

  @Override
  public float value() {
    return score().value();
  }

  @Override
  public Instant timestamp() {
    return score().timestamp();
  }

  @Override
  public Duration expiry() {
    return score().expiry();
  }
}

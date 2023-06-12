package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.time.Instant;
import org.immutables.value.Value;
import sharetrace.model.Identifiable;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.util.Identifiers;

@Value.Immutable
abstract class BaseRiskScoreMessage implements UserMessage, Identifiable, TemporalScore {

  @JsonUnwrapped
  @Value.Parameter
  public abstract RiskScore score();

  @JsonIgnore
  @Value.Parameter
  public abstract ActorRef<UserMessage> sender();

  @Override
  @Value.Default
  public String id() {
    return Identifiers.newLongString();
  }

  @Override
  public float value() {
    return score().value();
  }

  @Override
  public Instant timestamp() {
    return score().timestamp();
  }
}

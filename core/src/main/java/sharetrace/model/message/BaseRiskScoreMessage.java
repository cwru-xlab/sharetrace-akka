package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import org.immutables.value.Value;
import sharetrace.model.Identifiable;
import sharetrace.model.RiskScore;
import sharetrace.model.TemporalScore;
import sharetrace.util.IdFactory;

@Value.Immutable
abstract class BaseRiskScoreMessage implements UserMessage, Identifiable, TemporalScore {

  @JsonIgnore
  public abstract ActorRef<UserMessage> sender();

  @JsonIgnore
  protected abstract RiskScore score();

  @Override
  @Value.Default
  public String id() {
    return IdFactory.newLongString();
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

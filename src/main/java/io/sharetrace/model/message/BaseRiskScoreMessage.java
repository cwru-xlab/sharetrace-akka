package io.sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.sharetrace.model.Identifiable;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.TemporalScore;
import io.sharetrace.util.Identifiers;
import java.time.Instant;
import org.immutables.value.Value;

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

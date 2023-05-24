package io.sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.actor.UserActor;
import io.sharetrace.model.Identifiable;
import io.sharetrace.model.RiskScore;
import io.sharetrace.model.TemporalScore;
import io.sharetrace.util.Identifiers;
import java.time.Instant;
import org.immutables.value.Value;

/**
 * A uniquely identifiable message that contains the {@link RiskScore} of a {@link UserActor}.
 *
 * @see UserActor
 * @see RiskPropagation
 */
@Value.Immutable
abstract class BaseRiskScoreMessage implements UserMessage, Identifiable, TemporalScore {

  public static RiskScoreMessage of(RiskScore score, ActorRef<UserMessage> sender, String id) {
    return RiskScoreMessage.builder().score(score).sender(sender).id(id).build();
  }

  /** Returns the risk score contained in this message. */
  @Value.Parameter
  @JsonUnwrapped
  public abstract RiskScore score();

  /** Returns the actor reference associated with the {@link UserActor} that sent this message. */
  @Value.Parameter
  @JsonIgnore
  public abstract ActorRef<UserMessage> sender();

  /** Returns a unique identifier to track this message during {@link RiskPropagation}. */
  @Value.Default
  public String id() {
    return Identifiers.ofLongString();
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

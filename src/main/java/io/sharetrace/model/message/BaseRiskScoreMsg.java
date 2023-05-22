package io.sharetrace.model.message;

import akka.actor.typed.ActorRef;
import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.actor.UserActor;
import io.sharetrace.model.Identifiable;
import io.sharetrace.model.RiskScore;
import io.sharetrace.util.Identifiers;
import org.immutables.value.Value;

/**
 * A uniquely identifiable message that contains the {@link RiskScore} of a {@link UserActor}.
 *
 * @see UserActor
 * @see RiskPropagation
 */
@Value.Immutable
abstract class BaseRiskScoreMsg implements UserMsg, Identifiable, Comparable<RiskScoreMsg> {

  public static RiskScoreMsg of(RiskScore score, ActorRef<UserMsg> sender, String id) {
    return RiskScoreMsg.builder().score(score).sender(sender).id(id).build();
  }

  @Override
  public int compareTo(RiskScoreMsg msg) {
    return score().compareTo(msg.score());
  }

  /** Returns the risk score contained in this message. */
  @Value.Parameter
  public abstract RiskScore score();

  /** Returns the actor reference associated with the {@link UserActor} that sent this message. */
  @Value.Parameter
  public abstract ActorRef<UserMsg> sender();

  /** Returns a unique identifier to track this message during {@link RiskPropagation}. */
  @Value.Default
  public String id() {
    return Identifiers.ofLongString();
  }
}

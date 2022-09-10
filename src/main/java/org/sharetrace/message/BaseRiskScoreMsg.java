package org.sharetrace.message;

import akka.actor.typed.ActorRef;
import java.util.UUID;
import org.immutables.value.Value;
import org.sharetrace.actor.RiskPropagation;
import org.sharetrace.actor.User;
import org.sharetrace.model.RiskScore;

/**
 * A uniquely identifiable message that contains the {@link RiskScore} of a {@link User}.
 *
 * @see User
 * @see RiskPropagation
 */
@Value.Immutable
abstract class BaseRiskScoreMsg implements UserMsg, Comparable<RiskScoreMsg> {

  @Override
  public int compareTo(RiskScoreMsg msg) {
    return score().compareTo(msg.score());
  }

  /** Returns the risk score contained in this message. */
  public abstract RiskScore score();

  /** Returns the actor reference associated with the {@link User} that sent this message. */
  public abstract ActorRef<UserMsg> replyTo();

  /** Returns a unique identifier to track this message during {@link RiskPropagation}. */
  @Value.Default
  public String id() {
    return UUID.randomUUID().toString();
  }
}

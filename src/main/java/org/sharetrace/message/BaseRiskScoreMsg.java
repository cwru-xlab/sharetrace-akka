package org.sharetrace.message;

import akka.actor.typed.ActorRef;
import java.util.UUID;
import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.User;
import org.sharetrace.graph.ContactNetwork;

@Value.Immutable
abstract class BaseRiskScoreMsg implements UserMsg, Comparable<RiskScoreMsg> {

  @Override
  public int compareTo(RiskScoreMsg msg) {
    return score().compareTo(msg.score());
  }

  public abstract RiskScore score();

  /**
   * Returns the actor reference associated with the {@link User} that propagates this risk score.
   */
  public abstract ActorRef<UserMsg> replyTo();

  /**
   * Returns a universally unique identifier that can be used to correlate risk scores as they
   * propagate through the {@link ContactNetwork} during {@link RiskPropagation}.
   */
  @Value.Default
  public String id() {
    return UUID.randomUUID().toString();
  }
}
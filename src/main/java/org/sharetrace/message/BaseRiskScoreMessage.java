package org.sharetrace.message;

import akka.actor.typed.ActorRef;
import java.util.UUID;
import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.graph.ContactGraph;
import org.sharetrace.graph.Node;

@Value.Immutable
abstract class BaseRiskScoreMessage implements NodeMessage, Comparable<RiskScoreMessage> {

  /**
   * Returns the actor reference associated with the {@link Node} that propagates this risk score.
   */
  public abstract ActorRef<NodeMessage> replyTo();

  @Override
  public int compareTo(RiskScoreMessage message) {
    return score().compareTo(message.score());
  }

  public abstract RiskScore score();

  /**
   * Returns a universally unique identifier that can be used to correlate risk scores as they
   * propagate through the {@link ContactGraph} during {@link RiskPropagation}.
   */
  @Value.Default
  public String uuid() {
    return UUID.randomUUID().toString();
  }
}

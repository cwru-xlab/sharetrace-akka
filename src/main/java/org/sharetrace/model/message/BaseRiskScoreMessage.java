package org.sharetrace.model.message;

import akka.actor.typed.ActorRef;
import java.util.UUID;
import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.model.graph.ContactGraph;
import org.sharetrace.model.graph.Node;

@Value.Immutable
abstract class BaseRiskScoreMessage implements NodeMessage, Comparable<BaseRiskScoreMessage> {

  public abstract RiskScore score();

  /**
   * Returns the actor reference associated with the {@link Node} that propagates this risk score.
   */
  public abstract ActorRef<NodeMessage> replyTo();

  @Override
  public int compareTo(BaseRiskScoreMessage message) {
    return score().compareTo(message.score());
  }

  /**
   * Returns a universally unique identifier that can be used to correlate risk scores as they
   * propagate through the {@link ContactGraph} during {@link RiskPropagation}.
   */
  @Value.Default
  public String uuid() {
    return UUID.randomUUID().toString();
  }
}

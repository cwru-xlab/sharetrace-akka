package org.sharetrace.model.message;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import java.util.UUID;
import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.model.graph.ContactGraph;
import org.sharetrace.model.graph.Node;

@Value.Immutable
public interface RiskScoreMessage extends NodeMessage, Comparable<RiskScoreMessage> {

  static Builder builder() {
    return ImmutableRiskScoreMessage.builder();
  }

  RiskScore score();

  /**
   * Returns the actor reference associated with the {@link Node} that propagates this risk score.
   */
  ActorRef<NodeMessage> replyTo();

  @Override
  default int compareTo(RiskScoreMessage message) {
    return score().compareTo(message.score());
  }

  /**
   * Returns a universally unique identifier that can be used to correlate risk scores as they
   * propagate through the {@link ContactGraph} during {@link RiskPropagation}.
   */
  @Value.Default
  default String uuid() {
    return UUID.randomUUID().toString();
  }

  interface Builder {

    Builder score(RiskScore score);

    Builder score(double value, Instant timestamp);

    Builder replyTo(ActorRef<NodeMessage> replyTo);

    Builder uuid(String uuid);

    RiskScoreMessage build();
  }
}

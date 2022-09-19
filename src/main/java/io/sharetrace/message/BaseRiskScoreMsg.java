package io.sharetrace.message;

import akka.actor.typed.ActorRef;
import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.actor.User;
import io.sharetrace.model.RiskScore;
import java.util.concurrent.ThreadLocalRandom;
import org.immutables.value.Value;

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
    return String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE));
  }
}

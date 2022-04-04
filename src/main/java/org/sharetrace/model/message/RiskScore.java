package org.sharetrace.model.message;

import static org.sharetrace.util.Preconditions.checkArgument;
import akka.actor.typed.ActorRef;
import java.time.Instant;
import java.util.UUID;
import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.model.graph.ContactGraph;
import org.sharetrace.model.graph.Node;

/**
 * A timestamped probability of infection. As a message, a risk score is propagated through the
 * {@link ContactGraph}. When initially propagated from their source {@link Node}, a risk score is
 * referred to as a <b>symptom score</b> since it is only based on the symptoms of a person. As the
 * current value of a {@link Node}, a risk score is referred to as an <b>exposure score</b> since it
 * also accounts for (in)direct forms of contact with other persons.
 *
 * <p><b>Note</b>: this class has a natural ordering that is inconsistent with equals. Risk scores
 * are only compared by value (first) and timestamp (second).
 */
@Value.Immutable
public abstract class RiskScore implements NodeMessage, Comparable<RiskScore> {

  public static final double MIN_VALUE = 0d;
  public static final double MAX_VALUE = 1d;

  public static Builder builder() {
    return ImmutableRiskScore.builder();
  }

  /**
   * Returns the actor reference associated with the {@link Node} that propagates this risk score.
   */
  public abstract ActorRef<NodeMessage> replyTo();

  /** Returns the magnitude of this risk score; modified during {@link RiskPropagation}. */
  public abstract double value();

  /**
   * Returns when this risk score was first computed; never modified during {@link RiskPropagation}.
   */
  public abstract Instant timestamp();

  /**
   * Returns a universally unique identifier that can be used to correlate risk scores as they
   * propagate through the {@link ContactGraph} during {@link RiskPropagation}.
   */
  @Value.Default
  public String uuid() {
    return UUID.randomUUID().toString();
  }

  @Override
  public int compareTo(RiskScore score) {
    int byValue = Double.compare(value(), score.value());
    return byValue != 0 ? byValue : timestamp().compareTo(score.timestamp());
  }

  @Value.Check
  protected void check() {
    checkArgument(value() >= MIN_VALUE && value() <= MAX_VALUE, this::valueMessage);
  }

  private String valueMessage() {
    return "'value' must be between "
        + MIN_VALUE
        + " and "
        + MAX_VALUE
        + ", inclusive; got "
        + value();
  }

  public interface Builder {

    Builder replyTo(ActorRef<NodeMessage> replyTo);

    Builder value(double value);

    Builder timestamp(Instant timestamp);

    Builder uuid(String uuid);

    RiskScore build();
  }
}

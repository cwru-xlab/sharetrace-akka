package org.sharetrace.message;

import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.graph.ContactGraph;
import org.sharetrace.graph.Node;

import java.time.Instant;

import static org.sharetrace.util.Preconditions.checkInClosedRange;

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
abstract class BaseRiskScore implements Comparable<BaseRiskScore> {

  public static final double MIN_VALUE = 0d;
  public static final double MAX_VALUE = 1d;

  @Override
  public int compareTo(BaseRiskScore score) {
    int byValue = Double.compare(value(), score.value());
    return byValue != 0 ? byValue : timestamp().compareTo(score.timestamp());
  }

  /** Returns the magnitude of this risk score; modified during {@link RiskPropagation}. */
  @Value.Parameter
  public abstract double value();

  /**
   * Returns when this risk score was first computed; never modified during {@link RiskPropagation}.
   */
  @Value.Parameter
  public abstract Instant timestamp();

  @Value.Check
  protected void check() {
    checkInClosedRange(value(), MIN_VALUE, MAX_VALUE, "value");
  }
}

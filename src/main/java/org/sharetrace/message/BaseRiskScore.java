package org.sharetrace.message;

import static org.sharetrace.util.Preconditions.checkInClosedRange;
import java.time.Instant;
import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.User;
import org.sharetrace.graph.ContactNetwork;

/**
 * A timestamped probability of infection. As a message, a risk score is propagated through the
 * {@link ContactNetwork}. When initially propagated from their source {@link User}, a risk score is
 * referred to as a <b>symptom score</b> since it is only based on the symptoms of a person. As the
 * current value of a {@link User}, a risk score is referred to as an <b>exposure score</b> since it
 * also accounts for (in)direct forms of contact with other persons.
 *
 * <p><b>Note</b>: this class has a natural ordering that is inconsistent with equals. Risk scores
 * are only compared by value (first) and timestamp (second).
 */
@Value.Immutable
abstract class BaseRiskScore implements Comparable<RiskScore> {

  public static final float MIN_VALUE = 0f;
  public static final float MAX_VALUE = 1f;
  public static final float VALUE_RANGE = MAX_VALUE - MIN_VALUE;

  public static RiskScore ofMinValue(Instant timestamp) {
    return RiskScore.of(MIN_VALUE, timestamp);
  }

  @Override
  public int compareTo(RiskScore score) {
    int byValue = Float.compare(value(), score.value());
    return byValue != 0 ? byValue : timestamp().compareTo(score.timestamp());
  }

  /** Returns the magnitude of this risk score; modified during {@link RiskPropagation}. */
  @Value.Parameter
  public abstract float value();

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

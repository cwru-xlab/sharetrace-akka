package org.sharetrace.model;

import com.google.common.collect.ComparisonChain;
import java.time.Instant;
import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.User;
import org.sharetrace.message.RiskScoreMsg;
import org.sharetrace.util.Checks;

/**
 * A timestamped infection probability of a user. A risk score derived only from user symptoms, it
 * is called a <i>symptom score</i>. A risk score that also accounts for direct or indirect contact
 * that a user had with others is called an <i>exposure score</i>.
 *
 * @see RiskScoreMsg
 * @see User
 * @see RiskPropagation
 */
@Value.Immutable
abstract class BaseRiskScore implements Comparable<RiskScore> {

  public static final float MIN_VALUE = 0f;
  public static final float MAX_VALUE = 1f;
  public static final float VALUE_RANGE = MAX_VALUE - MIN_VALUE;

  /** Returns a risk score of minimum value and the given time. */
  public static RiskScore ofMinValue(Instant time) {
    return RiskScore.of(MIN_VALUE, time);
  }

  @Override
  public int compareTo(RiskScore score) {
    return ComparisonChain.start()
        .compare(value(), score.value())
        .compare(time(), score.time())
        .result();
  }

  /** Returns the probability of infection. */
  @Value.Parameter
  public abstract float value();

  /** Returns when this probability was first computed. */
  @Value.Parameter
  public abstract Instant time();

  @Value.Check
  protected void check() {
    Checks.inClosedRange(value(), MIN_VALUE, MAX_VALUE, "value");
    Checks.isAtLeast(time(), Instant.EPOCH, "time");
  }
}

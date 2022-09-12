package org.sharetrace.model;

import java.time.Duration;
import org.immutables.value.Value;
import org.sharetrace.actor.RiskPropagation;
import org.sharetrace.actor.User;
import org.sharetrace.message.RiskScoreMsg;
import org.sharetrace.util.Checks;

/**
 * Parameters that affect how and if messages are passed between {@link User}s.
 *
 * @see User
 * @see RiskPropagation
 */
@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseMsgParams {

  public static final double MIN_TRANS_RATE = 0d;
  public static final double MAX_TRANS_RATE = 1d;
  public static final double MIN_SEND_COEFF = 0d;
  public static final double MIN_TOLERANCE = 0d;

  /**
   * Returns the rate at which the value of a {@link RiskScore} exponentially decreases as it
   * propagates from the source {@link User} during {@link RiskPropagation}.
   */
  public abstract double transRate();

  /**
   * Returns the multiplier used to set the threshold of a {@link User} that determines if a
   * received {@link RiskScoreMsg} should be propagated. A {@link RiskScoreMsg} is eligible for
   * propagation if
   *
   * <pre>{@code scoreValue >= userValue * sendCoefficient}.</pre>
   *
   * Given a positive transmission rate, a positive coefficient guarantees that {@link
   * RiskPropagation} terminates.
   */
  public abstract double sendCoeff();

  /**
   * Returns the extent to which a {@link RiskScore} is relevant after a contact occurs. When
   * determining if a {@link RiskScoreMsg} is eligible for propagation, this is used to shift the
   * contact time forward. Thus, a {@link RiskScore} that was computed after a contact occurred may
   * still be eligible for propagation if
   *
   * <pre>{@code scoreTime <= contactTime + timeBuffer}.</pre>
   *
   * A nonzero value can account for delays in symptom onset and initial {@link User} communication.
   */
  public abstract Duration timeBuffer();

  /**
   * Returns the extent to which a {@link RiskScore} is relevant after it was computed. Intuitively,
   * an older {@link RiskScore} is less likely to reflect the current risk of a user and more likely
   * to already be accounted for via its propagation to other {@link User}s.
   */
  public abstract Duration scoreTtl();

  /**
   * Returns the extent to which a contact is relevant after it occurred. Intuitively, an older
   * contact is less likely to cause the propagation of a new {@link RiskScore} for which the
   * receiving {@link User}s had not already accounted.
   */
  public abstract Duration contactTtl();

  /**
   * Returns the maximum absolute difference between {@link RiskScore} values for them to be
   * approximately equal.
   */
  public abstract double tolerance();

  @Value.Check
  protected void check() {
    Checks.inOpen(transRate(), MIN_TRANS_RATE, MAX_TRANS_RATE, "transRate");
    Checks.isAtLeast(sendCoeff(), MIN_SEND_COEFF, "sendCoeff");
    Checks.isAtLeast(timeBuffer(), Duration.ZERO, "timeBuffer");
    Checks.isGreaterThan(scoreTtl(), Duration.ZERO, "scoreTtl");
    Checks.isGreaterThan(contactTtl(), Duration.ZERO, "contactTtl");
    Checks.isAtLeast(tolerance(), MIN_TOLERANCE, "tolerance");
  }
}

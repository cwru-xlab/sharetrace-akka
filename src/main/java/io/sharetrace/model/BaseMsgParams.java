package io.sharetrace.model;

import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.actor.UserActor;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.util.Checks;
import java.time.Duration;
import org.immutables.value.Value;

/**
 * Parameters that affect how and if messages are passed between {@link UserActor}s.
 *
 * @see UserActor
 * @see RiskPropagation
 */
@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseMsgParams {

  public static final float MIN_TRANS_RATE = 0f;
  public static final float MAX_TRANS_RATE = 1f;
  public static final float MIN_SEND_COEFF = 0f;
  public static final float MIN_TOLERANCE = 0f;

  @Value.Check
  protected void check() {
    Checks.inOpen(transRate(), MIN_TRANS_RATE, MAX_TRANS_RATE, "transRate");
    Checks.isAtLeast(sendCoeff(), MIN_SEND_COEFF, "sendCoeff");
    Checks.isAtLeast(timeBuffer(), Duration.ZERO, "timeBuffer");
    Checks.isGreaterThan(scoreTtl(), Duration.ZERO, "scoreTtl");
    Checks.isGreaterThan(contactTtl(), Duration.ZERO, "contactTtl");
    Checks.isAtLeast(tolerance(), MIN_TOLERANCE, "tolerance");
  }

  /**
   * Returns the rate at which the value of a {@link RiskScore} exponentially decreases as it
   * propagates from the source {@link UserActor} during {@link RiskPropagation}.
   */
  public abstract float transRate();

  /**
   * Returns the multiplier used to set the threshold of a {@link UserActor} that determines if a
   * received {@link RiskScoreMsg} should be propagated. A {@link RiskScoreMsg} is eligible for
   * propagation if
   *
   * <pre>{@code scoreValue >= userValue * sendCoefficient}.</pre>
   *
   * Given a positive transmission rate, a positive coefficient guarantees that {@link
   * RiskPropagation} terminates.
   */
  public abstract float sendCoeff();

  /**
   * Returns the extent to which a {@link RiskScore} is relevant after a contact occurs. When
   * determining if a {@link RiskScoreMsg} is eligible for propagation, this is used to shift the
   * contact time forward. Thus, a {@link RiskScore} that was computed after a contact occurred may
   * still be eligible for propagation if
   *
   * <pre>{@code scoreTime <= contactTime + timeBuffer}.</pre>
   *
   * A nonzero value can account for delays in symptom onset and initial {@link UserActor}
   * communication.
   */
  public abstract Duration timeBuffer();

  /**
   * Returns the extent to which a {@link RiskScore} is relevant after it was computed. Intuitively,
   * an older {@link RiskScore} is less likely to reflect the current risk of a user and more likely
   * to already be accounted for via its propagation to other {@link UserActor}s.
   */
  public abstract Duration scoreTtl();

  /**
   * Returns the extent to which a contact is relevant after it occurred. Intuitively, an older
   * contact is less likely to cause the propagation of a new {@link RiskScore} for which the
   * receiving {@link UserActor}s had not already accounted.
   */
  public abstract Duration contactTtl();

  /**
   * Returns the maximum absolute difference between {@link RiskScore} values for them to be
   * approximately equal.
   */
  public abstract float tolerance();
}

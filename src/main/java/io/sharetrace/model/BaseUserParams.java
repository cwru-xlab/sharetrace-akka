package io.sharetrace.model;

import com.google.common.collect.Range;
import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.actor.UserActor;
import io.sharetrace.message.RiskScoreMsg;
import io.sharetrace.util.Checks;
import java.time.Duration;
import org.immutables.value.Value;

/**
 * Parameters that modify the behavior of a {@link UserActor}.
 *
 * @see UserActor
 */
@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseUserParams {

  public static final float MIN_TRANS_RATE = 0f;
  public static final float MAX_TRANS_RATE = 1f;
  public static final float MIN_SEND_COEFF = 0f;
  public static final float MIN_TOLERANCE = 0f;
  public static final Duration MIN_TIME_BUFFER = Duration.ZERO;
  public static final Duration MIN_SCORE_TTL = Duration.ZERO;
  public static final Duration MIN_CONTACT_TTL = Duration.ZERO;
  public static final Duration MIN_IDLE_TIMEOUT = Duration.ZERO;

  private static final String TRANS_RATE = "transRate";
  private static final String SEND_COEFF = "sendCoeff";
  private static final String TIME_BUFFER = "timeBuffer";
  private static final String SCORE_TTL = "scoreTtl";
  private static final String CONTACT_TTL = "contactTtl";
  private static final String TOLERANCE = "tolerance";
  private static final String IDLE_TIMEOUT = "idleTimeout";
  private static final Range<Float> TRANS_RATE_RANGE = Range.open(MIN_TRANS_RATE, MAX_TRANS_RATE);
  private static final Range<Float> SEND_COEFF_RANGE = Range.atLeast(MIN_SEND_COEFF);
  private static final Range<Duration> TIME_BUFFER_RANGE = Range.atLeast(MIN_TIME_BUFFER);
  private static final Range<Duration> SCORE_TTL_RANGE = Range.greaterThan(MIN_SCORE_TTL);
  private static final Range<Duration> CONTACT_TTL_RANGE = Range.greaterThan(MIN_CONTACT_TTL);
  private static final Range<Float> TOLERANCE_RANGE = Range.atLeast(MIN_TOLERANCE);
  private static final Range<Duration> IDLE_TIMEOUT_RANGE = Range.greaterThan(MIN_IDLE_TIMEOUT);

  @Value.Check
  protected void check() {
    Checks.inRange(transRate(), TRANS_RATE_RANGE, TRANS_RATE);
    Checks.inRange(sendCoeff(), SEND_COEFF_RANGE, SEND_COEFF);
    Checks.inRange(timeBuffer(), TIME_BUFFER_RANGE, TIME_BUFFER);
    Checks.inRange(scoreTtl(), SCORE_TTL_RANGE, SCORE_TTL);
    Checks.inRange(contactTtl(), CONTACT_TTL_RANGE, CONTACT_TTL);
    Checks.inRange(tolerance(), TOLERANCE_RANGE, TOLERANCE);
    Checks.inRange(idleTimeout(), IDLE_TIMEOUT_RANGE, IDLE_TIMEOUT);
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

  /**
   * Returns the duration after which a {@link UserActor} terminates if it has not processed a
   * message.
   */
  public abstract Duration idleTimeout();
}

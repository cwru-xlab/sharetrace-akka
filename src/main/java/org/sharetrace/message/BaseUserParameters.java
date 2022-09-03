package org.sharetrace.message;

import java.time.Duration;
import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.User;
import org.sharetrace.graph.ContactNetwork;
import org.sharetrace.util.Checks;

/**
 * A collection of values that modify the behavior of a {@link User} while passing messages during
 * an execution of {@link RiskPropagation}.
 */
@Value.Immutable
abstract class BaseUserParameters implements UserMessage {

  public static final float MIN_TRANSMISSION_RATE = 0f;
  public static final float MAX_TRANSMISSION_RATE = 1f;
  public static final float MIN_SEND_COEFFICIENT = 0f;

  @Value.Check
  protected void check() {
    Checks.inClosedRange(
        transmissionRate(), MIN_TRANSMISSION_RATE, MAX_TRANSMISSION_RATE, "transmissionRate");
    Checks.atLeast(sendCoefficient(), MIN_SEND_COEFFICIENT, "sendCoefficient");
    Checks.atLeast(timeBuffer(), Duration.ZERO, "timeBuffer");
    Checks.greaterThan(scoreTtl(), Duration.ZERO, "scoreTtl");
    Checks.greaterThan(contactTtl(), Duration.ZERO, "contactTtl");
    Checks.greaterThan(idleTimeout(), Duration.ZERO, "idleTimeout");
    Checks.greaterThan(refreshPeriod(), Duration.ZERO, "refreshPeriod");
    Checks.atLeast(scoreTolerance(), 0f, "scoreTolerance");
  }

  /**
   * Returns the value which determines at what rate the value of a {@link RiskScore} exponentially
   * decreases as it propagates through the {@link ContactNetwork}. A transmission rate of 0 results
   * in no value decay, and so an execution of {@link RiskPropagation} must be terminated by other
   * means than relying on a nonzero send tolerance.
   */
  public abstract float transmissionRate();

  /**
   * Returns the value which determines the threshold that the value of a {@link RiskScore} must
   * satisfy to be propagated by a {@link User}. Specifically, a {@link RiskScore} is only eligible
   * for propagation if {@code scoreValue >= currentValue * sendCoefficient}. Given a positive
   * transmission rate that is less than 1, a positive send tolerance guarantees that asynchronous,
   * non-iterative message passing eventually terminates since the value of a propagated {@link
   * RiskScore} exponentially decreases with a rate constant equal to the transmission rate.
   */
  public abstract float sendCoefficient();

  /**
   * Returns the duration which determines to what extent a {@link RiskScore} is considered relevant
   * to a given contact after it occurred. A nonzero time buffer can account for delayed symptom
   * onset since a symptom-based {@link RiskScore} of a person would not begin to reflect the fact
   * that they are infected until after developing symptoms. Additionally, a nonzero time buffer can
   * account for the delay between when two persons come in contact and when their respective actors
   * begin to communicate.
   */
  public abstract Duration timeBuffer();

  /**
   * Returns the duration which determines how long a given {@link RiskScore} is considered
   * relevant. Intuitively, {@link RiskScore}s that are based on older data are (1) less likely to
   * still accurately reflect the current risk of a person; and (2) more likely to have already been
   * accounted for in the {@link ContactNetwork} by its propagation to other {@link User}s.
   */
  public abstract Duration scoreTtl();

  /**
   * Returns the duration which determines how long a given contact is considered relevant.
   * Intuitively, contacts that occurred further in the past are less likely to propagate any new
   * {@link RiskScore} in the {@link ContactNetwork} for which the complimentary {@link User} has
   * not already accounted.
   */
  public abstract Duration contactTtl();

  // TODO Add Javadoc
  public abstract Duration idleTimeout();

  // TODO Add Javadoc
  public abstract Duration refreshPeriod();

  /**
   * Returns the maximum absolute difference between {@link RiskScore} values for them to be
   * considered approximately equal.
   */
  public abstract float scoreTolerance();
}

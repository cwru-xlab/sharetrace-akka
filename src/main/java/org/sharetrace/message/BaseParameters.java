package org.sharetrace.message;

import static org.sharetrace.util.Preconditions.checkInClosedRange;
import static org.sharetrace.util.Preconditions.checkIsAtLeast;
import static org.sharetrace.util.Preconditions.checkIsNonNegative;
import static org.sharetrace.util.Preconditions.checkIsPositive;
import java.time.Duration;
import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.graph.ContactGraph;
import org.sharetrace.graph.Node;

/**
 * A collection of values that modify the behavior of a {@link Node} while passing messages during
 * an execution of {@link RiskPropagation}.
 */
@Value.Immutable
abstract class BaseParameters implements NodeMessage {

  public static final double MIN_TRANSMISSION_RATE = 0d;
  public static final double MAX_TRANSMISSION_RATE = 1d;
  public static final double MIN_SEND_TOLERANCE = 0d;

  @Value.Check
  protected void check() {
    checkInClosedRange(
        transmissionRate(), MIN_TRANSMISSION_RATE, MAX_TRANSMISSION_RATE, "transmissionRate");
    checkIsAtLeast(sendTolerance(), MIN_SEND_TOLERANCE, "sendTolerance");
    checkIsNonNegative(timeBuffer(), "timeBuffer");
    checkIsPositive(scoreTtl(), "scoreTtl");
    checkIsPositive(contactTtl(), "contactTtl");
    checkIsPositive(idleTimeout(), "idleTimeout");
    checkIsPositive(refreshRate(), "refreshRate");
  }

  /**
   * Returns the value which determines at what rate the value of a {@link RiskScore} exponentially
   * decreases as it propagates through the {@link ContactGraph}. A transmission rate of 0 results
   * in no value decay, and so an execution of {@link RiskPropagation} must be terminated by other
   * means than relying on a nonzero send tolerance.
   */
  public abstract double transmissionRate();

  /**
   * Returns the value which determines the threshold that the value of a {@link RiskScore} must
   * satisfy to be propagated by a {@link Node}. Specifically, a {@link RiskScore} is only eligible
   * for propagation if {@code scoreValue >= nodeValue * sendTolerance}. Given a positive
   * transmission rate that is less than 1, a positive send tolerance guarantees that asynchronous,
   * non-iterative message passing eventually terminates since the value of a propagated {@link
   * RiskScore} exponentially decreases with a rate constant equal to the transmission rate.
   */
  public abstract double sendTolerance();

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
   * accounted for in the {@link ContactGraph} by its propagation to other {@link Node}s.
   */
  public abstract Duration scoreTtl();

  /**
   * Returns the duration which determines how long a given contact is considered relevant.
   * Intuitively, contacts that occurred further in the past are less likely to propagate any new
   * {@link RiskScore} in the {@link ContactGraph} for which the complimentary {@link Node} has not
   * already accounted.
   */
  public abstract Duration contactTtl();

  // TODO Add Javadoc
  public abstract Duration idleTimeout();

  // TODO Add Javadoc
  public abstract Duration refreshRate();
}

package org.sharetrace.model.message;

import static org.sharetrace.util.Preconditions.checkInClosedRange;
import static org.sharetrace.util.Preconditions.checkIsAtLeast;
import static org.sharetrace.util.Preconditions.checkIsNonNegative;
import static org.sharetrace.util.Preconditions.checkIsPositive;
import java.time.Duration;
import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.model.graph.ContactGraph;
import org.sharetrace.model.graph.Node;

/**
 * A collection of values that modify the behavior of a {@link Node} while passing messages during
 * an execution of {@link RiskPropagation}.
 */
@Value.Immutable
public abstract class Parameters implements NodeMessage {

  public static final double MIN_TRANSMISSION_RATE = 0d;
  public static final double MAX_TRANSMISSION_RATE = 1d;
  public static final double MIN_SEND_TOLERANCE = 0d;

  public static Builder builder() {
    return ImmutableParameters.builder();
  }

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
   * Returns the value which determines at what rate the value of a {@link RiskScore} exponentially
   * decreases as it propagates through the {@link ContactGraph}. A transmission rate of 0 results
   * in no value decay, and so an execution of {@link RiskPropagation} must be terminated by other
   * means than relying on a nonzero send tolerance.
   */
  public abstract double transmissionRate();

  /**
   * Returns the duration which determines to what extent a {@link RiskScore} is considered relevant
   * to a given {@link Contact} after it occurred. A nonzero time buffer can account for delayed
   * symptom onset since a symptom-based {@link RiskScore} of a person would not begin to reflect
   * the fact that they are infected until after developing symptoms. Additionally, a nonzero time
   * buffer can account for the delay between when two persons come in contact and when their
   * respective actors begin to communicate.
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
   * Returns the duration which determines how long a given {@link Contact} is considered relevant.
   * Intuitively, {@link Contact}s that occurred further in the past are less likely to propagate
   * any new {@link RiskScore} in the {@link ContactGraph} for which the complimentary {@link Node}
   * has not already accounted.
   */
  public abstract Duration contactTtl();

  @Value.Check
  protected void check() {
    checkInClosedRange(
        transmissionRate(),
        MIN_TRANSMISSION_RATE,
        MAX_TRANSMISSION_RATE,
        this::transmissionRateMessage);
    checkIsAtLeast(sendTolerance(), MIN_SEND_TOLERANCE, this::sendToleranceMessage);
    checkIsNonNegative(timeBuffer(), this::timeBufferMessage);
    checkIsPositive(scoreTtl(), this::scoreTtlMessage);
    checkIsPositive(contactTtl(), this::contactTtlMessage);
  }

  private String transmissionRateMessage() {
    return "'transmissionRate' must be between "
        + MIN_TRANSMISSION_RATE
        + " and "
        + MAX_TRANSMISSION_RATE
        + ", inclusive; got "
        + transmissionRate();
  }

  private String sendToleranceMessage() {
    return "'sendTolerance' must be at least " + MIN_SEND_TOLERANCE + "; got " + sendTolerance();
  }

  private String timeBufferMessage() {
    return "'timeBuffer' must be non-negative; got " + timeBuffer();
  }

  private String scoreTtlMessage() {
    return "'scoreTtl' must be positive; got " + scoreTtl();
  }

  private String contactTtlMessage() {
    return "'contactTtl' must be positive; got " + contactTtl();
  }

  public interface Builder {

    Builder sendTolerance(double sendTolerance);

    Builder transmissionRate(double transmissionRate);

    Builder timeBuffer(Duration timeBuffer);

    Builder scoreTtl(Duration scoreTtl);

    Builder contactTtl(Duration contactTtl);

    Parameters build();
  }
}

package org.sharetrace.data.sampling;

import static org.sharetrace.util.Preconditions.checkIsPositive;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseTimeSampler extends BaseSampler<Instant> {

  @Override
  public Instant sample() {
    long lookBack = Math.round(normalizedSample(ttlDistribution()) * ttl().getSeconds());
    return referenceTime().minus(Duration.ofSeconds(lookBack));
  }

  /**
   * Returns a probability distribution over which to sample time-to-live percentages. Samples are
   * min-max normalized to ensure they are between 0 and 1.
   */
  @Value.Default
  protected RealDistribution ttlDistribution() {
    return new UniformRealDistribution(generator(), 0d, 1d);
  }

  /** Returns the duration for which a timestamp is valid. */
  protected abstract Duration ttl();

  /** Returns a timestamp to which all other timestamps be in reference */
  protected abstract Instant referenceTime();

  @Value.Check
  protected void check() {
    checkBoundedness(ttlDistribution());
    checkIsPositive(ttl(), "ttl");
  }
}

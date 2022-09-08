package org.sharetrace.data.sampling;

import java.time.Duration;
import java.time.Instant;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;
import org.sharetrace.util.Checks;
import org.sharetrace.util.TimeRef;

@Value.Immutable
abstract class BaseTimeSampler extends BaseSampler<Instant> implements TimeRef {

  @Override
  public Instant sample() {
    long lookBack = Math.round(normalizedSample(lookBacks()) * maxLookBack().getSeconds());
    return refTime().minusSeconds(lookBack);
  }

  /** Returns a timestamp to which all other timestamps be in reference */
  @Override
  public abstract Instant refTime();

  /**
   * Returns a probability distribution over which to sample time-to-live percentages. Samples are
   * min-max normalized to ensure they are between 0 and 1.
   */
  protected abstract RealDistribution lookBacks();

  /** Returns the duration for which a timestamp is valid. */
  protected abstract Duration maxLookBack();

  @Value.Check
  protected void check() {
    Checks.isGreaterThan(maxLookBack(), Duration.ZERO, "maxLookBack");
  }
}

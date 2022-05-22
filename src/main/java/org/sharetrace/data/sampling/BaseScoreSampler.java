package org.sharetrace.data.sampling;

import java.time.Instant;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.immutables.value.Value;
import org.sharetrace.message.RiskScore;

@Value.Immutable
abstract class BaseScoreSampler extends BaseSampler<RiskScore> {

  @Override
  public RiskScore sample() {
    return RiskScore.of(normalizedSample(valueDistribution()), timeSampler().sample());
  }

  /**
   * Returns a probability distribution over which to sample {@link RiskScore} values. Samples are
   * min-max normalized to ensure they are valid {@link RiskScore} values.
   */
  @Value.Default
  protected RealDistribution valueDistribution() {
    return new UniformRealDistribution(generator(), 0d, 1d);
  }

  /** Returns a sampler to generate {@link RiskScore} timestamps. */
  protected abstract Sampler<Instant> timeSampler();

  @Value.Check
  protected void check() {
    checkBoundedness(valueDistribution());
  }
}

package org.sharetrace.data.sampling;

import java.time.Instant;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.immutables.value.Value;
import org.sharetrace.message.RiskScore;

@Value.Immutable
abstract class BaseRiskScoreSampler extends BaseSampler<RiskScore> {

  @Override
  public RiskScore sample() {
    return RiskScore.of((float) normalizedSample(valueDistribution()), timeSampler().sample());
  }

  /**
   * Returns a probability distribution over which to sample {@link RiskScore} values. Samples are
   * min-max normalized to ensure they are valid {@link RiskScore} values.
   */
  @Value.Default
  protected RealDistribution valueDistribution() {
    return new UniformRealDistribution(generator(), RiskScore.MIN_VALUE, RiskScore.MAX_VALUE);
  }

  /** Returns a sampler to generate {@link RiskScore} timestamps. */
  protected abstract Sampler<Instant> timeSampler();

  @Value.Check
  protected void check() {
    checkBoundedness(valueDistribution());
  }
}

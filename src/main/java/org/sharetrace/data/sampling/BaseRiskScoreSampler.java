package org.sharetrace.data.sampling;

import java.time.Instant;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;
import org.sharetrace.message.RiskScore;

@Value.Immutable
abstract class BaseRiskScoreSampler extends BaseSampler<RiskScore> {

  @Override
  public RiskScore sample() {
    return RiskScore.builder()
        .value(RiskScore.VALUE_RANGE * normalizedSample(valueDistribution()))
        .timestamp(timeSampler().sample())
        .build();
  }

  /**
   * Returns a probability distribution over which to sample {@link RiskScore} values. Samples are
   * min-max normalized to ensure they are valid {@link RiskScore} values.
   */
  protected abstract RealDistribution valueDistribution();

  /** Returns a sampler to generate {@link RiskScore} timestamps. */
  protected abstract Sampler<Instant> timeSampler();
}

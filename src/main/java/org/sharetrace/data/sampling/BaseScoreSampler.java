package org.sharetrace.data.sampling;

import java.time.Instant;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.immutables.value.Value;
import org.sharetrace.message.RiskScore;

@Value.Immutable
abstract class BaseScoreSampler extends BaseSampler implements Sampler<RiskScore> {

  @Override
  public RiskScore sample() {
    return RiskScore.of(valueDistribution().sample(), timestampSampler().sample());
  }

  @Value.Default
  protected RealDistribution valueDistribution() {
    return new UniformRealDistribution(randomGenerator(), 0d, 1d);
  }

  protected abstract Sampler<Instant> timestampSampler();
}

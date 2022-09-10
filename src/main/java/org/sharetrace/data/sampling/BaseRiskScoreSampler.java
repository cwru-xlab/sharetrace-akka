package org.sharetrace.data.sampling;

import java.time.Instant;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;
import org.sharetrace.model.RiskScore;

@Value.Immutable
abstract class BaseRiskScoreSampler extends BaseSampler<RiskScore> {

  @Override
  public RiskScore sample() {
    return RiskScore.builder()
        .value(RiskScore.VALUE_RANGE * normalizedSample(values()))
        .time(timeSampler().sample())
        .build();
  }

  protected abstract RealDistribution values();

  protected abstract Sampler<Instant> timeSampler();
}

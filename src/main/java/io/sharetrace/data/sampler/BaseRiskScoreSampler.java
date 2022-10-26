package io.sharetrace.data.sampler;

import io.sharetrace.model.RiskScore;
import java.time.Instant;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseRiskScoreSampler extends BaseSampler<RiskScore> {

  @Override
  public RiskScore sample() {
    float scale = normalizedSample(values());
    return RiskScore.of(RiskScore.RANGE * scale, timeSampler().sample());
  }

  protected abstract RealDistribution values();

  protected abstract Sampler<Instant> timeSampler();
}

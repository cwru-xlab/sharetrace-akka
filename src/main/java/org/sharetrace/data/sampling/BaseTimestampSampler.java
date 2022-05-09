package org.sharetrace.data.sampling;

import java.time.Duration;
import java.time.Instant;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseTimestampSampler extends BaseSampler implements Sampler<Instant> {

  @Override
  public Instant sample() {
    double max = ttlDistribution().getSupportUpperBound();
    double min = ttlDistribution().getSupportLowerBound();
    double normalizedSample = (ttlDistribution().sample() - min) / (max - min);
    long lookBack = Math.round(normalizedSample * ttl().getSeconds());
    return referenceTime().minus(Duration.ofSeconds(lookBack));
  }

  @Value.Default
  protected RealDistribution ttlDistribution() {
    return new UniformRealDistribution(randomGenerator(), 0d, 1d);
  }

  protected abstract Duration ttl();

  protected abstract Instant referenceTime();
}

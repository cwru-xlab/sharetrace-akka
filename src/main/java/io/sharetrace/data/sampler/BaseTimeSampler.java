package io.sharetrace.data.sampler;

import io.sharetrace.model.TimeRef;
import io.sharetrace.util.Checks;
import java.time.Duration;
import java.time.Instant;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseTimeSampler extends BaseSampler<Instant> implements TimeRef {

  @Override
  public Instant sample() {
    long lookBack = Math.round(normalizedSample(lookBacks()) * maxLookBack().getSeconds());
    return refTime().minusSeconds(lookBack);
  }

  @Override
  public abstract Instant refTime();

  protected abstract RealDistribution lookBacks();

  protected abstract Duration maxLookBack();

  @Value.Check
  protected void check() {
    Checks.isGreaterThan(maxLookBack(), Duration.ZERO, "maxLookBack");
  }
}

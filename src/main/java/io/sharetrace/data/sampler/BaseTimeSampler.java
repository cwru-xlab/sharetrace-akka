package io.sharetrace.data.sampler;

import io.sharetrace.model.TimeRef;
import io.sharetrace.util.Checks;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseTimeSampler extends BaseSampler<Instant> implements TimeRef {

  @Override
  public Instant sample() {
    float scale = normalizedSample(lookBacks());
    long maxLookBack = maxLookBack().getSeconds();
    return refTime().minusSeconds(Math.round(scale * maxLookBack));
  }

  protected abstract RealDistribution lookBacks();

  protected abstract Duration maxLookBack();

  @Value.Check
  protected BaseTimeSampler check() {
    Checks.isGreaterThan(maxLookBack(), Duration.ZERO, "maxLookBack");
    return (refTime().getNano() != 0)
        ? TimeSampler.builder()
            .refTime(refTime().truncatedTo(ChronoUnit.SECONDS))
            .lookBacks(lookBacks())
            .maxLookBack(maxLookBack())
            .build()
        : this;
  }
}

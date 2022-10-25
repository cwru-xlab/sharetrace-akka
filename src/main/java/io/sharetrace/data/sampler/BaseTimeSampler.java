package io.sharetrace.data.sampler;

import com.google.common.collect.Range;
import io.sharetrace.model.TimeRef;
import io.sharetrace.util.Checks;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseTimeSampler extends BaseSampler<Instant> implements TimeRef {

  public static final Duration MIN_LOOK_BACK = Duration.ZERO;

  private static final Range<Duration> MAX_LOOK_BACK_RANGE = Range.greaterThan(MIN_LOOK_BACK);

  private static final String MAX_LOOK_BACK = "maxLookBack";

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
    Checks.inRange(maxLookBack(), MAX_LOOK_BACK_RANGE, MAX_LOOK_BACK);
    return (refTime().getNano() != 0)
        ? TimeSampler.builder()
            .refTime(refTime().truncatedTo(ChronoUnit.SECONDS))
            .lookBacks(lookBacks())
            .maxLookBack(maxLookBack())
            .build()
        : this;
  }
}

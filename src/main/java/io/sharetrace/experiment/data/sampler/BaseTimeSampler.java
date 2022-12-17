package io.sharetrace.experiment.data.sampler;

import com.google.common.collect.Range;
import io.sharetrace.model.TimeRef;
import io.sharetrace.util.Checks;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Value.Immutable
abstract class BaseTimeSampler extends AbstractSampler<Instant> implements TimeRef {

  public static final Duration MIN_LOOK_BACK = Duration.ZERO;

  private static final Range<Duration> MAX_LOOK_BACK_RANGE = Range.greaterThan(MIN_LOOK_BACK);

  @Override
  public Instant sample() {
    double lookBack = normalizedSample(lookBacks(), maxLookBack().toNanos());
    return refTime().minusNanos((long) lookBack).truncatedTo(ChronoUnit.SECONDS);
  }

  protected abstract RealDistribution lookBacks();

  protected abstract Duration maxLookBack();

  @Value.Check
  protected void check() {
    Checks.inRange(maxLookBack(), MAX_LOOK_BACK_RANGE, "maxLookBack");
  }
}

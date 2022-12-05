package io.sharetrace.experiment.data.sampler;

import com.google.common.collect.Range;
import io.sharetrace.model.TimeRef;
import io.sharetrace.util.Checks;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseTimeSampler extends AbstractSampler<Instant> implements TimeRef {

  public static final Duration MIN_LOOK_BACK = Duration.ZERO;

  private static final Range<Duration> MAX_LOOK_BACK_RANGE = Range.greaterThan(MIN_LOOK_BACK);

  @Override
  public Instant sample() {
    double lookBack = normalizedSample(lookBacks(), maxLookBack().toNanos());
    long rounded = Math.round(lookBack);
    return refTime().minusNanos(rounded).truncatedTo(ChronoUnit.SECONDS);
  }

  protected abstract RealDistribution lookBacks();

  protected abstract Duration maxLookBack();

  @Value.Check
  protected void check() {
    Checks.inRange(maxLookBack(), MAX_LOOK_BACK_RANGE, "maxLookBack");
  }
}

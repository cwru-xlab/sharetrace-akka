package io.sharetrace.experiment.data;

import com.google.common.collect.Range;
import io.sharetrace.model.TimestampReference;
import io.sharetrace.util.Checks;
import io.sharetrace.util.DistributedRandom;
import java.time.Duration;
import java.time.Instant;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseRandomTimestampFactory implements TimestampFactory, TimestampReference {

  public static final Duration MIN_BACKWARD_RANGE = Duration.ZERO;

  private static final Range<Duration> BACKWARD_RANGE = Range.greaterThan(MIN_BACKWARD_RANGE);

  @Override
  public Instant getTimestamp() {
    long backwardNanos = random().nextLong(backwardRange().toNanos());
    return referenceTimestamp().minusNanos(backwardNanos);
  }

  protected abstract DistributedRandom random();

  protected abstract Duration backwardRange();

  @Value.Check
  protected void check() {
    Checks.checkRange(backwardRange(), BACKWARD_RANGE, "backwardRange");
  }
}

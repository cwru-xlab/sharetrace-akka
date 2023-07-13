package sharetrace.experiment.data;

import com.google.common.collect.Range;
import java.time.Duration;
import java.time.Instant;
import org.immutables.value.Value;
import sharetrace.model.TimestampReference;
import sharetrace.util.Checks;
import sharetrace.util.DistributedRandom;

@Value.Immutable
abstract class BaseRandomTimestampFactory implements TimestampFactory, TimestampReference {

  @Override
  public Instant getTimestamp() {
    long backwardNanos = random().nextLong(backwardRange().toNanos());
    return referenceTimestamp().minusNanos(backwardNanos);
  }

  protected abstract DistributedRandom random();

  protected abstract Duration backwardRange();

  @Value.Check
  protected void check() {
    Checks.checkRange(backwardRange(), Range.greaterThan(Duration.ZERO), "backwardRange");
  }
}

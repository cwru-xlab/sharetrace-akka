package sharetrace.model.factory;

import com.google.common.collect.Range;
import java.time.Duration;
import java.time.Instant;
import sharetrace.Buildable;
import sharetrace.model.Timestamped;
import sharetrace.util.Checks;
import sharetrace.util.DistributedRandom;

@Buildable
public record RandomTimeFactory(DistributedRandom random, Duration range, Instant timestamp)
    implements TimeFactory, Timestamped {

  public RandomTimeFactory {
    Checks.checkRange(range, Range.atLeast(Duration.ZERO), "range");
  }

  @Override
  public Instant getTime() {
    var offset = random.nextLong(range.toNanos());
    return timestamp.minusNanos(offset);
  }
}

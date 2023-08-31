package sharetrace.model.factory;

import com.google.common.collect.Range;
import java.time.Duration;
import java.time.Instant;
import sharetrace.Buildable;
import sharetrace.model.Timestamped;
import sharetrace.util.DistributedRandom;
import sharetrace.util.Ranges;

@Buildable
public record RandomTimeFactory(DistributedRandom random, Duration range, Instant timestamp)
    implements TimeFactory, Timestamped {

  public RandomTimeFactory {
    Ranges.check("range", range, Range.atLeast(Duration.ZERO));
  }

  @Override
  public Instant getTime() {
    var offset = random.nextLong(range.toMillis());
    return timestamp.minusMillis(offset);
  }
}

package sharetrace.model.factory;

import com.google.common.collect.Range;
import java.time.Duration;
import java.time.Instant;
import sharetrace.Buildable;
import sharetrace.model.Timestamped;
import sharetrace.util.DistributedRandom;
import sharetrace.util.Ranges;

@Buildable
public record RandomTimeFactory(DistributedRandom random, Duration period, Instant referenceTime)
    implements TimeFactory {

  public RandomTimeFactory {
    Ranges.check("period", period, Range.atLeast(Duration.ZERO));
    Ranges.check("referenceTime", referenceTime, Range.atLeast(Timestamped.MIN_TIME));
  }

  @Override
  public Instant getTime() {
    var offset = random.nextLong(period.toMillis());
    return referenceTime.minusMillis(offset);
  }
}

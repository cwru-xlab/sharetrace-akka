package sharetrace.model.factory;

import com.google.common.collect.Range;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import sharetrace.Buildable;
import sharetrace.model.Timestamp;
import sharetrace.util.DistributedRandom;
import sharetrace.util.Ranges;

@Buildable
public record RandomTimeFactory(DistributedRandom random, Duration period, Timestamp referenceTime)
    implements TimeFactory {

  public RandomTimeFactory {
    Ranges.check("period", period, Range.atLeast(Duration.ZERO));
  }

  @Override
  public Timestamp getTime() {
    var offset = random.nextLong(period.toMillis());
    return referenceTime.minus(offset, ChronoUnit.MILLIS);
  }
}

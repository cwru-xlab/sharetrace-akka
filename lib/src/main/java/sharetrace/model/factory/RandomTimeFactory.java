package sharetrace.model.factory;

import com.google.common.collect.Range;
import sharetrace.Buildable;
import sharetrace.model.Ranges;
import sharetrace.model.random.DistributedRandom;

@Buildable
public record RandomTimeFactory(DistributedRandom distribution, long period, long referenceTime)
    implements TimeFactory {

  public RandomTimeFactory {
    Ranges.check("period", period, Range.atLeast(0L));
  }

  @Override
  public long getTime() {
    return Math.subtractExact(referenceTime, distribution.nextLong(period));
  }
}

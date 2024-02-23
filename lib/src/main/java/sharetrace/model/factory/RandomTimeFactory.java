package sharetrace.model.factory;

import com.google.common.collect.Range;
import sharetrace.Buildable;
import sharetrace.model.DistributedRandom;
import sharetrace.model.Ranges;

@Buildable
public record RandomTimeFactory(DistributedRandom random, long period, long referenceTime)
    implements TimeFactory {

  public RandomTimeFactory {
    Ranges.check("period", period, Range.atLeast(0L));
  }

  @Override
  public long getTime() {
    return Math.subtractExact(referenceTime, random.nextLong(period));
  }
}

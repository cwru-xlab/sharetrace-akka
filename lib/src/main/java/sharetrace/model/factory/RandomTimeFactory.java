package sharetrace.model.factory;

import com.google.common.collect.Range;
import sharetrace.Buildable;
import sharetrace.model.Ranges;
import sharetrace.model.random.DistributedRandom;

@Buildable
public record RandomTimeFactory(DistributedRandom random, long period, long referenceTime)
    implements TimeFactory {

  public RandomTimeFactory {
    Ranges.check("referenceTime", referenceTime, Range.atLeast(0L));
    Ranges.check("period", period, Range.closed(0L, referenceTime));
  }

  @Override
  public long getTime() {
    return Math.subtractExact(referenceTime, random.nextLong(period));
  }

  @Override
  public String type() {
    return "Random";
  }
}

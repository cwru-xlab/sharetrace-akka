package sharetrace.model.factory;

import com.google.common.collect.Range;
import sharetrace.Buildable;
import sharetrace.util.DistributedRandom;
import sharetrace.util.Ranges;

@Buildable
public record RandomTimeFactory(DistributedRandom random, long period, long referenceTime)
    implements TimeFactory {

  public RandomTimeFactory {
    Ranges.check("period", period, Range.atLeast(0L));
  }

  @Override
  public long getTime() {
    var offset = random.nextLong(period);
    return Math.subtractExact(referenceTime, offset);
  }
}

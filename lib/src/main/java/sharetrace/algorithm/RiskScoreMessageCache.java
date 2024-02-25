package sharetrace.algorithm;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.time.InstantSource;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BinaryOperator;
import sharetrace.model.message.RiskScoreMessage;

final class RiskScoreMessageCache extends Cache<RiskScoreMessage> {

  private static final Comparator<RiskScoreMessage> COMPARATOR = Comparator.naturalOrder();
  private static final BinaryOperator<RiskScoreMessage> MERGER = BinaryOperator.maxBy(COMPARATOR);

  private final RangeMap<Long, RiskScoreMessage> cache;

  public RiskScoreMessageCache(InstantSource timeSource) {
    super(timeSource);
    this.cache = TreeRangeMap.create();
  }

  public Optional<RiskScoreMessage> max(Range<Long> range) {
    refresh();
    return values(cache.subRangeMap(range)).stream().max(COMPARATOR);
  }

  @Override
  public void add(RiskScoreMessage value) {
    var key = Range.closedOpen(value.timestamp(), value.expiryTime());
    cache.merge(key, value, MERGER);
    super.add(value);
  }

  @Override
  protected Collection<RiskScoreMessage> values() {
    return values(cache);
  }

  private Collection<RiskScoreMessage> values(RangeMap<Long, RiskScoreMessage> cache) {
    return cache.asMapOfRanges().values();
  }
}

package sharetrace.algorithm;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.time.InstantSource;
import java.util.Collection;
import java.util.Optional;
import sharetrace.model.message.RiskScoreMessage;

final class RiskScoreMessageCache extends Cache<RiskScoreMessage> {

  private final RangeMap<Long, RiskScoreMessage> cache;

  public RiskScoreMessageCache(InstantSource timeSource) {
    super(timeSource);
    this.cache = TreeRangeMap.create();
  }

  public Optional<RiskScoreMessage> max() {
    return max(Range.all());
  }

  public Optional<RiskScoreMessage> max(long olderThan) {
    return max(Range.lessThan(olderThan));
  }

  @Override
  public boolean add(RiskScoreMessage value) {
    var key = Range.closedOpen(value.timestamp(), value.expiryTime());
    cache.merge(key, value, merger);
    return super.add(value);
  }

  @Override
  public RiskScoreMessageCache refresh() {
    return (RiskScoreMessageCache) super.refresh();
  }

  @Override
  protected Collection<RiskScoreMessage> values() {
    return values(cache);
  }

  private Optional<RiskScoreMessage> max(Range<Long> range) {
    return values(cache.subRangeMap(range)).stream().max(comparator);
  }

  private Collection<RiskScoreMessage> values(RangeMap<Long, RiskScoreMessage> cache) {
    return cache.asMapOfRanges().values();
  }
}

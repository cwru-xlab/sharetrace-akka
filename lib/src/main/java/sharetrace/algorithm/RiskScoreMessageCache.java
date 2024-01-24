package sharetrace.algorithm;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.time.InstantSource;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BinaryOperator;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.util.Cache;

final class RiskScoreMessageCache implements Cache<RiskScoreMessage> {

  private static final Comparator<RiskScoreMessage> COMPARATOR = Comparator.naturalOrder();
  private static final BinaryOperator<RiskScoreMessage> MERGER = BinaryOperator.maxBy(COMPARATOR);

  private final InstantSource timeSource;
  private final RangeMap<Long, RiskScoreMessage> cache;

  private Range<Long> minKey;

  public RiskScoreMessageCache(InstantSource timeSource) {
    this.timeSource = timeSource;
    this.cache = TreeRangeMap.create();
    updateMinKey();
  }

  @Override
  public Optional<RiskScoreMessage> max() {
    return max(Range.all());
  }

  @Override
  public Optional<RiskScoreMessage> max(long atMost) {
    return max(Range.atMost(atMost));
  }

  @Override
  public RiskScoreMessageCache refresh() {
    var currentTime = timeSource.millis();
    if (!minKey.contains(currentTime)) {
      cache.remove(Range.lessThan(currentTime));
      updateMinKey();
    }
    return this;
  }

  @Override
  public void add(RiskScoreMessage value) {
    var key = Range.closedOpen(value.timestamp(), value.expiryTime());
    cache.merge(key, value, MERGER);
    updateMinKey();
  }

  @Override
  @SuppressWarnings("NullableProblems")
  public Iterator<RiskScoreMessage> iterator() {
    return Iterators.unmodifiableIterator(cache.asMapOfRanges().values().iterator());
  }

  private Optional<RiskScoreMessage> max(Range<Long> range) {
    return cache.subRangeMap(range).asMapOfRanges().values().stream().max(COMPARATOR);
  }

  private void updateMinKey() {
    minKey = Iterables.getFirst(cache.asMapOfRanges().keySet(), Range.all());
  }
}

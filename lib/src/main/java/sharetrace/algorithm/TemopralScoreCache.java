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
import sharetrace.model.TemporalScore;
import sharetrace.util.Cache;

final class TemopralScoreCache<V extends TemporalScore> implements Cache<V> {

  private final InstantSource timeSource;
  private final RangeMap<Long, V> cache;
  private final Comparator<? super V> comparator;
  private final BinaryOperator<V> merger;

  private Range<Long> minKey;

  public TemopralScoreCache(InstantSource timeSource) {
    this.timeSource = timeSource;
    this.comparator = Comparator.naturalOrder();
    this.merger = BinaryOperator.maxBy(comparator);
    this.cache = TreeRangeMap.create();
    updateMinKey();
  }

  @Override
  public Optional<V> max() {
    return max(Range.all());
  }

  @Override
  public Optional<V> max(long atMost) {
    return max(Range.atMost(atMost));
  }

  @Override
  public TemopralScoreCache<V> refresh() {
    var currentTime = timeSource.millis();
    if (!minKey.contains(currentTime)) {
      cache.remove(Range.lessThan(currentTime));
      updateMinKey();
    }
    return this;
  }

  @Override
  public void add(V value) {
    var key = Range.closedOpen(value.timestamp(), value.expiryTime());
    cache.merge(key, value, merger);
    updateMinKey();
  }

  @Override
  @SuppressWarnings("NullableProblems")
  public Iterator<V> iterator() {
    return Iterators.unmodifiableIterator(cache.asMapOfRanges().values().iterator());
  }

  private Optional<V> max(Range<Long> range) {
    return cache.subRangeMap(range).asMapOfRanges().values().stream().max(comparator);
  }

  private void updateMinKey() {
    minKey = Iterables.getFirst(cache.asMapOfRanges().keySet(), Range.all());
  }
}

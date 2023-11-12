package sharetrace.cache;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BinaryOperator;
import sharetrace.model.Expirable;
import sharetrace.model.Timestamped;

public final class RangeCache<V extends Expirable & Timestamped & Comparable<? super V>>
    implements Cache<V> {

  private final RangeMap<Instant, V> cache;
  private final InstantSource timeSource;
  private final Comparator<? super V> comparator;
  private final BinaryOperator<V> merger;
  private Range<Instant> min;

  public RangeCache(InstantSource timeSource) {
    this.timeSource = timeSource;
    this.comparator = Comparator.naturalOrder();
    this.merger = BinaryOperator.maxBy(comparator);
    this.cache = TreeRangeMap.create();
    updateMin();
  }

  @Override
  public Optional<V> max() {
    return max(Range.all());
  }

  @Override
  public Optional<V> max(Instant atMost) {
    return max(Range.atMost(atMost));
  }

  @Override
  public RangeCache<V> refresh() {
    var currentTime = timeSource.instant();
    if (!min.contains(currentTime)) {
      cache.remove(Range.lessThan(currentTime));
      updateMin();
    }
    return this;
  }

  @Override
  public void add(V value) {
    var key = Range.closedOpen(value.timestamp(), value.expiryTime());
    cache.merge(key, value, merger);
    updateMin();
  }

  @Override
  @SuppressWarnings("NullableProblems")
  public Iterator<V> iterator() {
    return Iterators.unmodifiableIterator(cache.asMapOfRanges().values().iterator());
  }

  private Optional<V> max(Range<Instant> range) {
    return cache.subRangeMap(range).asMapOfRanges().values().stream().max(comparator);
  }

  private void updateMin() {
    min = Iterables.getFirst(cache.asMapOfRanges().keySet(), Range.all());
  }
}

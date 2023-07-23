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

@SuppressWarnings({"UnstableApiUsage", "BooleanMethodIsAlwaysInverted", "NullableProblems"})
public final class RangeCache<V extends Expirable & Comparable<? super V>>
    implements Cache<Range<Instant>, V> {

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
    var now = timeSource.instant();
    if (!min.contains(now)) {
      cache.remove(Range.lessThan(now));
      updateMin();
    }
    return this;
  }

  @Override
  public void put(Range<Instant> key, V value) {
    cache.merge(key, value, merger);
    updateMin();
  }

  @Override
  public void add(V value) {
    put(Range.closedOpen(value.timestamp(), value.expiresAt()), value);
  }

  @Override
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

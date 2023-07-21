package sharetrace.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.BinaryOperator;
import org.immutables.builder.Builder;
import sharetrace.model.Expirable;

@SuppressWarnings({"UnstableApiUsage", "BooleanMethodIsAlwaysInverted", "NullableProblems"})
public final class RangeCache<V extends Expirable> implements Iterable<V> {

  private final Clock clock;
  private final Comparator<? super V> comparator;
  private final BinaryOperator<V> merger;
  private final RangeMap<Instant, V> cache;

  private Range<Instant> min;

  @Builder.Constructor
  RangeCache(Clock clock, Comparator<? super V> comparator, BinaryOperator<V> merger) {
    this.clock = clock;
    this.comparator = comparator;
    this.merger = merger;
    this.cache = TreeRangeMap.create();
    updateMin();
  }

  public Optional<V> max() {
    return max(cache);
  }

  public Optional<V> max(Instant atMost) {
    return max(cache.subRangeMap(Range.atMost(atMost)));
  }

  public RangeCache<V> refresh() {
    Instant now = clock.instant();
    if (!min.contains(now)) {
      cache.remove(Range.lessThan(now));
      updateMin();
    }
    return this;
  }

  public void add(V value) {
    Range<Instant> key = Range.closedOpen(value.timestamp(), value.expiresAt());
    cache.merge(key, value, merger);
    updateMin();
  }

  public Iterator<V> iterator() {
    return Iterators.unmodifiableIterator(cache.asMapOfRanges().values().iterator());
  }

  private Optional<V> max(RangeMap<Instant, V> cache) {
    return cache.asMapOfRanges().values().stream().max(comparator);
  }

  private void updateMin() {
    min = Iterators.getNext(cache.asMapOfRanges().keySet().iterator(), Range.all());
  }
}

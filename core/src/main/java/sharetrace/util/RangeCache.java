package sharetrace.util;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import org.immutables.builder.Builder;
import sharetrace.model.Expirable;

@SuppressWarnings({"UnstableApiUsage", "BooleanMethodIsAlwaysInverted", "NullableProblems"})
public final class RangeCache<V extends Expirable> implements Iterable<V> {

  private final Clock clock;
  private final Duration expiry;
  private final Comparator<? super V> comparator;
  private final BinaryOperator<V> merger;
  private final RangeMap<Instant, V> cache;

  private Range<Instant> minKey;

  @Builder.Constructor
  RangeCache(
      Clock clock, Duration expiry, Comparator<? super V> comparator, BinaryOperator<V> merger) {
    this.clock = clock;
    this.expiry = expiry;
    this.comparator = comparator;
    this.merger = merger;
    this.cache = TreeRangeMap.create();
  }

  public Optional<V> max() {
    return max(clock.instant());
  }

  public Optional<V> max(Instant atMost) {
    RangeMap<Instant, V> subCache = cache.subRangeMap(Range.atMost(atMost));
    return subCache.asMapOfRanges().values().stream().max(comparator);
  }

  public RangeCache<V> refresh() {
    Instant min = clock.instant().minus(expiry);
    if (!minKey.contains(min)) {
      cache.remove(Range.lessThan(min));
      updateMinKey();
    }
    return this;
  }

  public void add(V value) {
    cache.merge(getKey(value), value, merger);
    updateMinKey();
  }

  @Override
  public Iterator<V> iterator() {
    return cache.asMapOfRanges().values().iterator();
  }

  private Range<Instant> getKey(V value) {
    Instant end = value.timestamp();
    Instant start = end.minus(value.expiry());
    return Range.openClosed(start, end);
  }

  private void updateMinKey() {
    Set<Range<Instant>> keys = cache.asMapOfRanges().keySet();
    minKey = keys.isEmpty() ? Range.all() : keys.iterator().next();
  }
}

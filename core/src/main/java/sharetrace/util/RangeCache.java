package sharetrace.util;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.BinaryOperator;
import org.immutables.builder.Builder;
import sharetrace.model.Expirable;

@SuppressWarnings({"UnstableApiUsage"})
public final class RangeCache<V extends Expirable> {

  private final Clock clock;
  private final Duration expiry;
  private final Comparator<? super V> comparator;
  private final BinaryOperator<V> merger;
  private final RangeMap<Instant, V> cache;

  private Range<Instant> minKey;

  private RangeCache(
      Clock clock, Duration expiry, Comparator<? super V> comparator, BinaryOperator<V> merger) {
    this.clock = clock;
    this.expiry = expiry;
    this.comparator = comparator;
    this.merger = merger;
    this.cache = TreeRangeMap.create();
  }

  @Builder.Factory
  static <V extends Expirable> RangeCache<V> rangeCache(
      Clock clock, Comparator<? super V> comparator, BinaryOperator<V> merger, Duration expiry) {
    return new RangeCache<>(clock, expiry, comparator, merger);
  }

  public Optional<V> max() {
    return max(clock.instant());
  }

  public Optional<V> max(Instant atMost) {
    RangeMap<Instant, V> subCache = cache.subRangeMap(Range.atMost(atMost));
    return subCache.asMapOfRanges().values().stream().max(comparator);
  }

  public void refresh() {
    if (!isEmpty()) {
      Instant min = clock.instant().minus(expiry);
      if (!minKey.contains(min)) {
        cache.remove(Range.lessThan(min));
        updateMinKey();
      }
    }
  }

  public void put(V value) {
    if (value.isAlive(clock)) {
      cache.merge(getKey(value), value, merger);
      updateMinKey();
    }
  }

  private Range<Instant> getKey(V value) {
    Instant end = value.timestamp();
    Instant start = end.minus(value.expiry());
    return Range.openClosed(start, end);
  }

  private void updateMinKey() {
    minKey = isEmpty() ? Range.all() : cache.asMapOfRanges().entrySet().iterator().next().getKey();
  }

  private boolean isEmpty() {
    return cache.asMapOfRanges().isEmpty();
  }
}

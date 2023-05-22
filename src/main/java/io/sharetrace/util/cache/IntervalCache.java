package io.sharetrace.util.cache;

import com.google.common.collect.Range;
import io.sharetrace.util.Checks;
import io.sharetrace.util.Collecting;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

public final class IntervalCache<V extends Comparable<? super V>> {

  private final Map<Long, V> cache;
  private final BinaryOperator<V> mergeStrategy;
  private final Clock clock;
  private final long interval;
  private final long lookBack;
  private final long lookAhead;
  private final long refreshPeriod;
  private long lastRefresh;
  private long rangeStart;
  private Range<Instant> range;

  private IntervalCache(CacheParams<V> parameters) {
    cache = Collecting.newLongKeyedHashMap();
    mergeStrategy = parameters.mergeStrategy();
    clock = parameters.clock();
    interval = toLong(parameters.interval());
    lookBack = interval * (parameters.numIntervals() - parameters.numLookAhead());
    lookAhead = interval * parameters.numLookAhead();
    refreshPeriod = toLong(parameters.refreshPeriod());
    lastRefresh = toLong(Instant.MIN);
  }

  public static <V extends Comparable<? super V>> IntervalCache<V> create(
      CacheParams<V> parameters) {
    return new IntervalCache<>(parameters);
  }

  public Optional<V> get(Instant key) {
    refresh();
    return Optional.ofNullable(cache.get(floorKey(key)));
  }

  public void put(Instant key, V value) {
    refresh();
    cache.merge(checkedFloorKey(key), value, mergeStrategy);
  }

  public Optional<V> max(Instant key) {
    refresh();
    return cache.entrySet().stream()
        .filter(isNotAfter(key))
        .map(Map.Entry::getValue)
        .max(Comparator.naturalOrder());
  }

  private void refresh() {
    long now = toLong(clock.instant());
    if (now - lastRefresh > refreshPeriod) {
      rangeStart = now - lookBack;
      long rangeEnd = now + lookAhead;
      range = Range.closedOpen(toInstant(rangeStart), toInstant(rangeEnd));
      cache.entrySet().removeIf(isExpired());
      lastRefresh = now;
    }
  }

  private Predicate<Map.Entry<Long, ?>> isExpired() {
    return entry -> entry.getKey() < rangeStart;
  }

  private Predicate<Map.Entry<Long, ?>> isNotAfter(Instant instant) {
    long key = floorKey(instant);
    return entry -> entry.getKey() <= key;
  }

  private long checkedFloorKey(Instant key) {
    return floorKey(Checks.checkRange(key, range, "key"));
  }

  private long floorKey(Instant key) {
    return floorKey(toLong(key));
  }

  private long floorKey(long key) {
    return rangeStart + interval * Math.floorDiv(key - rangeStart, interval);
  }

  private static long toLong(Duration duration) {
    return duration.getSeconds();
  }

  private static long toLong(Instant instant) {
    return instant.getEpochSecond();
  }

  private static Instant toInstant(long epochSeconds) {
    return Instant.ofEpochSecond(epochSeconds);
  }
}

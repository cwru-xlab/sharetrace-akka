package sharetrace.util.cache;

import com.google.common.collect.Range;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import sharetrace.util.Checks;
import sharetrace.util.Collecting;

public final class IntervalCache<V extends Comparable<? super V>> {

  private final Map<Long, V> cache;
  private final BinaryOperator<V> mergeStrategy;
  private final Clock clock;
  private final long interval;
  private final long backwardRange;
  private final long forwardRange;
  private final long refreshPeriod;

  private long lastRefresh;
  private long rangeStart;
  private Range<Instant> range;

  private IntervalCache(CacheParameters<V> parameters) {
    cache = Collecting.newLongKeyedHashMap();
    mergeStrategy = parameters.mergeStrategy();
    clock = parameters.clock();
    interval = toLong(parameters.interval());
    backwardRange = computeBackwardRange(interval, parameters);
    forwardRange = computeForwardRange(interval, parameters);
    refreshPeriod = toLong(parameters.refreshPeriod());
    lastRefresh = -1;
  }

  public static <V extends Comparable<? super V>> IntervalCache<V> create(
      CacheParameters<V> parameters) {
    return new IntervalCache<>(parameters);
  }

  private static long computeBackwardRange(long interval, CacheParameters<?> parameters) {
    return Math.multiplyExact(interval, parameters.intervals() - parameters.forwardIntervals());
  }

  private static long computeForwardRange(long interval, CacheParameters<?> parameters) {
    return Math.multiplyExact(interval, parameters.forwardIntervals());
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
        .filter(isBefore(key))
        .map(Map.Entry::getValue)
        .max(Comparator.naturalOrder());
  }

  private void refresh() {
    long now = now();
    if (now - lastRefresh > refreshPeriod) {
      rangeStart = now - backwardRange;
      long rangeEnd = now + forwardRange;
      range = Range.closedOpen(toInstant(rangeStart), toInstant(rangeEnd));
      cache.entrySet().removeIf(isExpired());
      lastRefresh = now();
    }
  }

  private Predicate<Map.Entry<Long, ?>> isExpired() {
    return entry -> entry.getKey() < rangeStart;
  }

  private long now() {
    return toLong(clock.instant());
  }

  private Predicate<Map.Entry<Long, ?>> isBefore(Instant instant) {
    long key = floorKey(instant);
    return entry -> entry.getKey() < key;
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
}

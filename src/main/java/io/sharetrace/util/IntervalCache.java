package io.sharetrace.util;

import com.google.common.collect.Range;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;

/**
 * A cache that lazily maintains a finite number of contiguous, half-closed (start-inclusive) time
 * intervals in which values are cached. The timespan of the cache is defined as {@code nIntervals *
 * intervalDuration}. This implementation differs from a standard cache in that the time-to-live of
 * a value is based on its specified timestamp, and not how long it has existed in the cache.
 *
 * <p>Upon request to retrieve a value, the cache checks to see if it should synchronously refresh
 * itself by shifting forward its time horizon and removing expired values (based on their
 * timestamp). The frequency with which this refresh operation occurs is based on the specified
 * {@code refreshPeriod} and {@code clock}. Note that if all values have expired, the cache is
 * reinitialized based on the current time, according to the {@code clock}.
 *
 * <p>A user-defined strategy can be provided that determines how values in a given time interval
 * are merged. By default, the new value unconditionally replaces the old value.
 *
 * @param <T> The type of the cached values.
 */
public final class IntervalCache<T extends Comparable<T>> {

  private static final String TEMPORAL = "temporal";
  private final Map<Long, T> cache;
  private final BinaryOperator<T> mergeStrategy;
  private final Clock clock;
  private final long interval;
  private final long lookBack;
  private final long lookAhead;
  private final long refreshPeriod;
  private long lastRefresh;
  private long rangeStart;
  private Range<Long> range;

  private IntervalCache(CacheParams<T> params) {
    cache = new Long2ObjectOpenHashMap<>();
    mergeStrategy = params.mergeStrategy();
    clock = params.clock();
    interval = getLong(params.interval());
    lookBack = interval * (params.numIntervals() - params.numLookAhead());
    lookAhead = interval * params.numLookAhead();
    refreshPeriod = getLong(params.refreshPeriod());
    lastRefresh = getLong(Instant.MIN);
  }

  private static long getLong(TemporalAmount temporalAmount) {
    return temporalAmount.get(ChronoUnit.SECONDS);
  }

  private static long getLong(Temporal temporal) {
    return temporal.getLong(ChronoField.INSTANT_SECONDS);
  }

  public static <T extends Comparable<T>> IntervalCache<T> create(CacheParams<T> params) {
    return new IntervalCache<>(params);
  }

  /**
   * Returns an {@link Optional} containing the cached value associated with the time interval that
   * contains the specified timestamp. If no value has been cached in the time interval or the
   * timestamp falls outside the timespan of the cache an empty {@link Optional} is returned. Prior
   * to retrieving the value, the cache is possibly refreshed if it has been sufficiently long since
   * its previous refresh.
   */
  public Optional<T> get(Temporal temporal) {
    refresh();
    long key = floorKey(getLong(temporal));
    return Optional.ofNullable(cache.get(key));
  }

  private void refresh() {
    long now = getTime();
    if (now - lastRefresh > refreshPeriod) {
      rangeStart = now - lookBack;
      long rangeEnd = now + lookAhead;
      range = Range.closedOpen(rangeStart, rangeEnd);
      cache.entrySet().removeIf(isExpired());
      lastRefresh = now;
    }
  }

  private long floorKey(long temporal) {
    return rangeStart + interval * Math.floorDiv(temporal - rangeStart, interval);
  }

  private long getTime() {
    return getLong(clock.instant());
  }

  private Predicate<Map.Entry<Long, ?>> isExpired() {
    return entry -> entry.getKey() < rangeStart;
  }

  /**
   * Adds the specified value to the time interval that contains the specified timestamp according
   * merge strategy of this instance. Prior to adding the value, the cache is possibly refreshed if
   * it has been sufficiently long since its previous refresh. This method follows {@link
   * Map#merge(Object, Object, BiFunction)} with the exception that null values are not permitted.
   *
   * @throws IllegalArgumentException if the timespan does not contain the specified timestamp.
   */
  public void put(Temporal temporal, T value) {
    refresh();
    long key = checkedFloorKey(temporal);
    T oldValue = cache.get(key);
    T newValue = (oldValue == null) ? value : mergeStrategy.apply(oldValue, value);
    cache.put(key, newValue);
  }

  private long checkedFloorKey(Temporal temporal) {
    return floorKey(Checks.inRange(getLong(temporal), range, TEMPORAL));
  }

  /**
   * Returns an {@link Optional} containing the maximum value, according to the specified
   * comparator, whose time interval contains specified timestamp. If no values have been cached in
   * the time intervals that end prior to the timestamp, an empty {@link Optional} is returned.
   * Prior to retrieving the value, the cache is possibly refreshed if it has been sufficiently long
   * since its previous refresh.
   */
  public Optional<T> max(Temporal temporal) {
    refresh();
    return cache.entrySet().stream()
        .filter(isNotAfter(temporal))
        .map(Map.Entry::getValue)
        .max(Comparator.naturalOrder());
  }

  private Predicate<Map.Entry<Long, ?>> isNotAfter(Temporal temporal) {
    long key = floorKey(getLong(temporal));
    return entry -> entry.getKey() < key;
  }
}

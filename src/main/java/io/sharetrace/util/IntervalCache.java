package io.sharetrace.util;

import com.google.common.collect.Range;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
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
public final class IntervalCache<T> {

  public static final int MIN_INTERVALS = 1;
  public static final int MIN_LOOK_AHEAD = 0;
  private static final String TEMPORAL = "temporal";
  private final Map<Long, T> cache;
  private final BinaryOperator<T> mergeStrategy;
  private final Comparator<T> comparator;
  private final Clock clock;
  private final long interval;
  private final long lookBack;
  private final long lookAhead;
  private final long refreshPeriod;
  private long lastRefresh;
  private long rangeStart;
  private Range<Long> range;

  private IntervalCache(Builder<T> builder) {
    cache = builder.cache;
    mergeStrategy = builder.mergeStrategy;
    comparator = builder.comparator;
    clock = builder.clock;
    interval = toLong(builder.interval);
    lookBack = toLong(builder.lookBack);
    lookAhead = toLong(builder.lookAhead);
    refreshPeriod = toLong(builder.refreshPeriod);
    lastRefresh = toLong(Instant.MIN);
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  private static long toLong(Duration duration) {
    return duration.getSeconds();
  }

  private static long toLong(Instant time) {
    return time.getEpochSecond();
  }

  private static Predicate<Entry<Long, ?>> isNotAfter(Instant time) {
    long t = toLong(time);
    return entry -> entry.getKey() <= t;
  }

  /**
   * Returns an {@link Optional} containing the cached value associated with the time interval that
   * contains the specified timestamp. If no value has been cached in the time interval or the
   * timestamp falls outside the timespan of the cache an empty {@link Optional} is returned. Prior
   * to retrieving the value, the cache is possibly refreshed if it has been sufficiently long since
   * its previous refresh.
   */
  public Optional<T> get(Instant time) {
    refresh();
    long key = floorKey(toLong(time));
    return Optional.ofNullable(cache.get(key));
  }

  /**
   * Adds the specified value to the time interval that contains the specified timestamp according
   * merge strategy of this instance. Prior to adding the value, the cache is possibly refreshed if
   * it has been sufficiently long since its previous refresh. This method follows {@link
   * Map#merge(Object, Object, BiFunction)} with the exception that null values are not permitted.
   *
   * @throws IllegalArgumentException if the timespan does not contain the specified timestamp.
   */
  public void put(Instant time, T value) {
    refresh();
    long key = checkedFloorKey(toLong(time));
    T oldValue = cache.get(key);
    T newValue = (oldValue == null) ? value : mergeStrategy.apply(oldValue, value);
    cache.put(key, newValue);
  }

  /**
   * Returns an {@link Optional} containing the maximum value, according to the specified
   * comparator, whose time interval contains specified timestamp. If no values have been cached in
   * the time intervals that end prior to the timestamp, an empty {@link Optional} is returned.
   * Prior to retrieving the value, the cache is possibly refreshed if it has been sufficiently long
   * since its previous refresh.
   */
  public Optional<T> max(Instant time) {
    refresh();
    return cache.entrySet().stream().filter(isNotAfter(time)).map(Entry::getValue).max(comparator);
  }

  private void refresh() {
    if (getTime() - lastRefresh > refreshPeriod) {
      rangeStart = getTime() - lookBack;
      long rangeEnd = getTime() + lookAhead;
      range = Range.closedOpen(rangeStart, rangeEnd);
      cache.entrySet().removeIf(isExpired());
      lastRefresh = getTime();
    }
  }

  private Predicate<Entry<Long, ?>> isExpired() {
    return entry -> entry.getKey() < rangeStart;
  }

  private long floorKey(long time) {
    return rangeStart + interval * Math.floorDiv(time - rangeStart, interval);
  }

  private long checkedFloorKey(long time) {
    return floorKey(Checks.inRange(time, range, TEMPORAL));
  }

  private long getTime() {
    return toLong(clock.instant());
  }

  public static final class Builder<T> {

    private BinaryOperator<T> mergeStrategy;
    private Comparator<T> comparator;
    private Clock clock;
    private Duration interval;
    private int numIntervals;
    private int numLookAhead;
    private Duration lookBack;
    private Duration lookAhead;
    private Duration refreshPeriod;
    private Map<Long, T> cache;

    public Builder<T> interval(Duration interval) {
      this.interval = interval;
      return this;
    }

    public Builder<T> numIntervals(int numIntervals) {
      this.numIntervals = numIntervals;
      return this;
    }

    public Builder<T> numLookAhead(int numLookAhead) {
      this.numLookAhead = numLookAhead;
      return this;
    }

    public Builder<T> refreshPeriod(Duration refreshPeriod) {
      this.refreshPeriod = refreshPeriod;
      return this;
    }

    public Builder<T> clock(Clock clock) {
      this.clock = clock;
      return this;
    }

    public Builder<T> mergeStrategy(BinaryOperator<T> mergeStrategy) {
      this.mergeStrategy = mergeStrategy;
      return this;
    }

    public Builder<T> comparator(Comparator<T> comparator) {
      this.comparator = comparator;
      return this;
    }

    public IntervalCache<T> build() {
      checkFields();
      lookBack = interval.multipliedBy(numIntervals - numLookAhead);
      lookAhead = interval.multipliedBy(numLookAhead);
      cache = new Long2ObjectOpenHashMap<>();
      return new IntervalCache<>(this);
    }

    private void checkFields() {
      Checks.isNotNull(interval, "interval");
      Checks.isNotNull(refreshPeriod, "refreshPeriod");
      Checks.isNotNull(clock, "clock");
      Checks.isNotNull(mergeStrategy, "mergeStrategy");
      Checks.isNotNull(comparator, "comparator");
      Checks.isAtLeast(interval, Duration.ZERO, "interval");
      Checks.isAtLeast(numIntervals, MIN_INTERVALS, "numIntervals");
      Checks.inClosedOpen(numLookAhead, MIN_LOOK_AHEAD, numIntervals, "numLookAhead");
      Checks.isAtLeast(refreshPeriod, Duration.ZERO, "refreshPeriod");
    }
  }
}

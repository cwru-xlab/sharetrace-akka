package org.sharetrace.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
public class IntervalCache<T extends Comparable<T>> {

  public static final int MIN_INTERVALS = 1;
  public static final int DEFAULT_INTERVALS = MIN_INTERVALS;
  public static final int MIN_LOOK_AHEAD = 0;
  public static final int DEFAULT_LOOK_AHEAD = MIN_INTERVALS;
  public static final Duration DEFAULT_REFRESH_PERIOD = Duration.ofMinutes(1L);
  public static final Duration DEFAULT_INTERVAL = Duration.ofDays(1L);
  public static final Clock DEFAULT_CLOCK = Clock.systemUTC();

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
  private long rangeEnd;

  private IntervalCache(Builder<T> builder) {
    cache = builder.cache;
    mergeStrategy = builder.mergeStrategy;
    comparator = builder.comparator;
    clock = builder.clock;
    interval = toLong(builder.interval);
    lookBack = toLong(builder.lookBack);
    lookAhead = toLong(builder.lookAhead);
    refreshPeriod = toLong(builder.refreshPeriod);
    lastRefresh = getTime();
    updateRange();
  }

  private static long toLong(Duration duration) {
    return duration.toSeconds();
  }

  private long getTime() {
    return toLong(clock.instant());
  }

  private void updateRange() {
    long now = getTime();
    rangeStart = now - lookBack;
    rangeEnd = now + lookAhead;
  }

  private static long toLong(Instant instant) {
    return instant.getEpochSecond();
  }

  public static <T extends Comparable<T>> Builder<T> builder() {
    return new Builder<>();
  }

  public static <T> BinaryOperator<T> defaultMergeStrategy() {
    return (oldValue, newValue) -> newValue;
  }

  /**
   * Returns an {@link Optional} containing the cached value associated with the time interval that
   * contains the specified timestamp. If no value has been cached in the time interval or the
   * timestamp falls outside the timespan of the cache an empty {@link Optional} is returned. Prior
   * to retrieving the value, the cache is possibly refreshed if it has been sufficiently long since
   * its previous refresh.
   */
  public Optional<T> get(Instant timestamp) {
    Objects.requireNonNull(timestamp);
    refresh();
    long key = floorKey(toLong(timestamp));
    return Optional.ofNullable(cache.get(key));
  }

  private void refresh() {
    if (getTime() - lastRefresh > refreshPeriod) {
      updateRange();
      cache.entrySet().removeIf(isExpired());
      lastRefresh = getTime();
    }
  }

  private long floorKey(long timestamp) {
    return rangeStart + interval * Math.floorDiv(timestamp - rangeStart, interval);
  }

  private Predicate<Entry<Long, ?>> isExpired() {
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
  public void put(Instant timestamp, T value) {
    Objects.requireNonNull(timestamp);
    refresh();
    long key = checkedFloorKey(toLong(timestamp));
    T oldValue = cache.get(key);
    T newValue = oldValue == null ? value : mergeStrategy.apply(oldValue, value);
    cache.put(key, newValue);
  }

  private long checkedFloorKey(long timestamp) {
    return floorKey(Checks.closedOpen(timestamp, rangeStart, rangeEnd, "timestamp"));
  }

  /**
   * Returns an {@link Optional} containing the maximum value, according to the specified
   * comparator, whose time interval contains specified timestamp. If no values have been cached in
   * the time intervals that end prior to the timestamp, an empty {@link Optional} is returned.
   * Prior to retrieving the value, the cache is possibly refreshed if it has been sufficiently long
   * since its previous refresh.
   */
  public Optional<T> max(Instant timestamp) {
    Objects.requireNonNull(timestamp);
    refresh();
    return cache.entrySet().stream()
        .filter(isNotAfter(timestamp))
        .map(Entry::getValue)
        .max(comparator);
  }

  private static Predicate<Entry<Long, ?>> isNotAfter(Instant timestamp) {
    long time = toLong(timestamp);
    return entry -> entry.getKey() <= time;
  }

  public static final class Builder<T extends Comparable<T>> {

    private BinaryOperator<T> mergeStrategy = defaultMergeStrategy();
    private Comparator<T> comparator;
    private Clock clock = DEFAULT_CLOCK;
    private Duration interval = DEFAULT_INTERVAL;
    private int nIntervals = DEFAULT_INTERVALS;
    private int nLookAhead = DEFAULT_LOOK_AHEAD;
    private Duration lookBack;
    private Duration lookAhead;
    private Duration refreshPeriod = DEFAULT_REFRESH_PERIOD;
    private Map<Long, T> cache;

    /** Sets duration of each contiguous time interval. */
    public Builder<T> interval(Duration interval) {
      this.interval = interval;
      return this;
    }

    /** Sets the total number of contiguous time intervals. */
    public Builder<T> nIntervals(int nIntervals) {
      this.nIntervals = nIntervals;
      return this;
    }

    /** Sets the number of "future" time intervals. */
    public Builder<T> nLookAhead(int nLookAhead) {
      this.nLookAhead = nLookAhead;
      return this;
    }

    /** Sets the duration after which the cache will refresh. */
    public Builder<T> refreshPeriod(Duration refreshPeriod) {
      this.refreshPeriod = refreshPeriod;
      return this;
    }

    /** Sets the clock that the cache will use for its notion of time. */
    public Builder<T> clock(Clock clock) {
      this.clock = clock;
      return this;
    }

    /** Sets the strategy that will be used when adding values to the cache. */
    public Builder<T> mergeStrategy(BinaryOperator<T> mergeStrategy) {
      this.mergeStrategy = mergeStrategy;
      return this;
    }

    /** Sets the {@link Comparator} that will be used when comparing values in the cache. */
    public Builder<T> comparator(Comparator<T> comparator) {
      this.comparator = comparator;
      return this;
    }

    /** Returns an initialized instance of the cache. */
    public IntervalCache<T> build() {
      checkFields();
      lookBack = interval.multipliedBy(nIntervals - nLookAhead);
      lookAhead = interval.multipliedBy(nLookAhead);
      cache = new Long2ObjectOpenHashMap<>();
      return new IntervalCache<>(this);
    }

    private void checkFields() {
      Objects.requireNonNull(interval);
      Objects.requireNonNull(refreshPeriod);
      Objects.requireNonNull(clock);
      Objects.requireNonNull(mergeStrategy);
      Objects.requireNonNull(comparator);
      Checks.atLeast(interval, Duration.ZERO, "interval");
      Checks.atLeast(nIntervals, MIN_INTERVALS, "nIntervals");
      Checks.closedOpen(nLookAhead, MIN_LOOK_AHEAD, nIntervals, "nLookAhead");
      Checks.atLeast(refreshPeriod, Duration.ZERO, "refreshPeriod");
    }
  }
}

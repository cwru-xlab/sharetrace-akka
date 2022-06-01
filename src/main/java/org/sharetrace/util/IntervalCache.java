package org.sharetrace.util;

import static org.sharetrace.util.Preconditions.checkInLowerInclusiveRange;
import static org.sharetrace.util.Preconditions.checkIsAtLeast;
import static org.sharetrace.util.Preconditions.checkIsPositive;
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
public class IntervalCache<T> {

  public static final int MIN_INTERVALS = 1;
  public static final int DEFAULT_INTERVALS = MIN_INTERVALS;
  public static final int MIN_LOOK_AHEAD = 0;
  public static final int DEFAULT_LOOK_AHEAD = MIN_INTERVALS;
  public static final Duration DEFAULT_REFRESH_RATE = Duration.ofMinutes(1L);
  public static final Duration DEFAULT_INTERVAL = Duration.ofDays(1L);
  public static final Clock DEFAULT_CLOCK = Clock.systemUTC();

  private final Map<Long, T> cache;
  private final BinaryOperator<T> mergeStrategy;
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
    clock = builder.clock;
    interval = builder.interval;
    lookBack = builder.lookBack;
    lookAhead = builder.lookAhead;
    refreshPeriod = builder.refreshPeriod;
    lastRefresh = getTime();
    updateRange();
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

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public static <T> BinaryOperator<T> defaultMergeStrategy() {
    return (oldValue, newValue) -> newValue;
  }

  private static long toLong(Duration duration) {
    return duration.toSeconds();
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
   * @throws NullPointerException if the value being added to the cache is null.
   */
  public void put(Instant timestamp, T value) {
    Objects.requireNonNull(timestamp);
    Objects.requireNonNull(value);
    refresh();
    long key = checkedFloorKey(toLong(timestamp));
    T oldValue = cache.get(key);
    T newValue = oldValue == null ? value : mergeStrategy.apply(oldValue, value);
    cache.put(key, Objects.requireNonNull(newValue));
  }

  private long checkedFloorKey(long timestamp) {
    return floorKey(checkInLowerInclusiveRange(timestamp, rangeStart, rangeEnd, "timestamp"));
  }

  /**
   * Returns an {@link Optional} containing the maximum value, according to the specified
   * comparator, whose time interval contains specified timestamp. If no values have been cached in
   * the time intervals that end prior to the timestamp, an empty {@link Optional} is returned.
   * Prior to retrieving the value, the cache is possibly refreshed if it has been sufficiently long
   * since its previous refresh.
   */
  public Optional<T> max(Instant timestamp, Comparator<T> comparator) {
    Objects.requireNonNull(timestamp);
    Objects.requireNonNull(comparator);
    refresh();
    long time = toLong(timestamp);
    return cache.entrySet().stream().filter(isNotAfter(time)).map(Entry::getValue).max(comparator);
  }

  private static Predicate<Entry<Long, ?>> isNotAfter(long timestamp) {
    return entry -> entry.getKey() <= timestamp;
  }

  public static final class Builder<T> {

    private BinaryOperator<T> mergeStrategy = defaultMergeStrategy();
    private Clock clock = DEFAULT_CLOCK;
    private long interval = toLong(DEFAULT_INTERVAL);
    private int nIntervals = DEFAULT_INTERVALS;
    private int nLookAhead = DEFAULT_LOOK_AHEAD;
    private long lookBack;
    private long lookAhead;
    private long refreshPeriod = toLong(DEFAULT_REFRESH_RATE);
    private Map<Long, T> cache;

    /** Sets duration of each contiguous time interval. */
    public Builder<T> interval(Duration interval) {
      this.interval = toLong(checkIsPositive(interval, "interval"));
      return this;
    }

    /** Sets the total number of contiguous time intervals. */
    public Builder<T> nIntervals(int nIntervals) {
      this.nIntervals = checkIsAtLeast(nIntervals, MIN_INTERVALS, "nIntervals");
      return this;
    }

    /** Sets the number of "future" time intervals. */
    public Builder<T> nLookAhead(int nLookAhead) {
      this.nLookAhead = nLookAhead;
      return this;
    }

    /** Sets the duration after which the cache will refresh. */
    public Builder<T> refreshPeriod(Duration refreshPeriod) {
      this.refreshPeriod = toLong(checkIsPositive(refreshPeriod, "refreshPeriod"));
      return this;
    }

    /** Sets the clock that the cache will use for its notion of time. */
    public Builder<T> clock(Clock clock) {
      this.clock = Objects.requireNonNull(clock);
      return this;
    }

    /** Sets the strategy that will be used when adding values to the cache. */
    public Builder<T> mergeStrategy(BinaryOperator<T> mergeStrategy) {
      this.mergeStrategy = Objects.requireNonNull(mergeStrategy);
      return this;
    }

    /** Returns an initialized instance of the cache. */
    public IntervalCache<T> build() {
      checkInLowerInclusiveRange(nLookAhead, MIN_LOOK_AHEAD, nIntervals, "nLookAhead");
      lookBack = interval * (nIntervals - nLookAhead);
      lookAhead = interval * nLookAhead;
      cache = newCache();
      return new IntervalCache<>(this);
    }

    private Map<Long, T> newCache() {
      return new Long2ObjectOpenHashMap<>();
    }
  }
}

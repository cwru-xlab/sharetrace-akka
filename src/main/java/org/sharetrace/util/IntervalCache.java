package org.sharetrace.util;

import static org.sharetrace.util.Preconditions.checkArgument;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import javax.annotation.Nullable;

/**
 * A cache that maintains a finite number of contiguous time intervals in which values are cached.
 * The timespan of the cache is defined as {@code nIntervals * intervalDuration}. This
 * implementation differs from a standard cache in that the time-to-live of a value is based on its
 * specified timestamp, and not how long it has existed in the cache.
 *
 * <p>On each call to {@link #get(Instant)} and {@link #put(Instant, Object)}, the cache checks to
 * see if it should synchronously refresh itself by shifting forward its time horizon and removing
 * expired time intervals and their associated values. The frequency with which this refresh
 * operation occurs is based on the specified {@code refreshRate} and {@code clock}. Note that if
 * all values have expired, the cache is reinitialized based on the current time, according to the
 * {@code clock}.
 *
 * <p>A user-defined strategy can be provided that determines how values in a given time interval
 * are merged. By default, the new value unconditionally replaces the old value.
 *
 * @param <T> The type of the cached values.
 */
public class IntervalCache<T> {

  private static final long MIN_INTERVALS = 1L;
  private final NavigableMap<Instant, T> cache;
  private final BinaryOperator<T> mergeStrategy;
  private final Supplier<Instant> clock;
  private final Duration interval;
  private final long nIntervals;
  private final Duration span;
  private final Duration refreshRate;
  private Instant lastRefresh;
  private Instant rangeStart;
  private Instant startOfRangeEnd;
  private Instant rangeEnd;

  private IntervalCache(Builder<T> builder) {
    this.cache = builder.cache;
    this.mergeStrategy = builder.mergeStrategy;
    this.clock = builder.clock;
    this.interval = builder.interval;
    this.nIntervals = builder.nIntervals;
    this.span = builder.span;
    this.refreshRate = builder.refreshRate;
    this.lastRefresh = clock.get();
    updateRange();
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  public Builder<T> toBuilder() {
    return new Builder<>(this);
  }

  /**
   * Returns the cached value associated with the time interval that contains the specified
   * timestamp . If the timestamp falls outside the timespan of the cache, {@code null} is returned.
   * Prior to retrieving the value, the cache is possibly refreshed if it has been sufficiently long
   * since its previous refresh.
   */
  @Nullable
  public T get(Instant timestamp) {
    Objects.requireNonNull(timestamp);
    refresh();
    T value = null;
    if (timestamp.isBefore(rangeEnd)) {
      Entry<Instant, T> entry = cache.floorEntry(timestamp);
      value = entry == null ? null : entry.getValue();
    }
    return value;
  }

  /**
   * Adds the specified value to the time interval that contains the specified timestamp according
   * merge strategy of this instance. Prior to adding the value, the cache is possibly refreshed if
   * it has been sufficiently long since its previous refresh.
   *
   * @throws IllegalArgumentException if the timespan does not contain the specified timestamp.
   */
  public void put(Instant timestamp, T value) {
    Objects.requireNonNull(timestamp);
    Objects.requireNonNull(value);
    refresh();
    Instant key = cache.floorKey(checkInRange(timestamp));
    cache.merge(key, value, mergeStrategy);
  }

  /**
   * Returns the maximum value, according to the specified comparator, whose time interval contains
   * or occurs before the specified timestamp. No maximum may be found if no value has been cached
   * in the time intervals before or during the specified timestamp. In this case, {@code null} is
   * returned.
   */
  @Nullable
  public T headMax(Instant timestamp, Comparator<T> comparator) {
    Objects.requireNonNull(timestamp);
    Objects.requireNonNull(comparator);
    return cache.headMap(timestamp, true).values().stream()
        .filter(Objects::nonNull)
        .max(comparator)
        .orElse(null);
  }

  private Instant checkInRange(Instant timestamp) {
    boolean inRange = rangeStart.isBefore(timestamp) && rangeEnd.isAfter(timestamp);
    checkArgument(inRange, () -> rangeMessage(timestamp));
    return timestamp;
  }

  private String rangeMessage(Instant timestamp) {
    return "'timestamp' must be between " + rangeStart + " and " + rangeEnd + "; got " + timestamp;
  }

  private void refresh() {
    Duration sinceRefresh = Duration.between(lastRefresh, clock.get());
    if (sinceRefresh.compareTo(refreshRate) > 0) {
      long nExpired = removeExpired();
      extend(nExpired);
      updateRange();
      lastRefresh = clock.get();
    }
  }

  private long removeExpired() {
    Instant expiredAt = clock.get().minus(span);
    NavigableSet<Instant> expired = cache.navigableKeySet().headSet(expiredAt, true);
    long nExpired = expired.size();
    expired.clear();
    return nExpired;
  }

  private void extend(long nIntervals) {
    if (cache.isEmpty()) {
      cache.putAll(toBuilder().build().cache);
    } else {
      LongStream.rangeClosed(1, nIntervals)
          .mapToObj(i -> startOfRangeEnd.plus(interval.multipliedBy(i)))
          .forEach(timestamp -> cache.put(timestamp, null));
    }
  }

  private void updateRange() {
    rangeStart = cache.firstKey();
    startOfRangeEnd = cache.lastKey();
    rangeEnd = startOfRangeEnd.plus(interval);
  }

  public static final class Builder<T> {

    private final NavigableMap<Instant, T> cache;
    private BinaryOperator<T> mergeStrategy;
    private Supplier<Instant> clock;
    private Duration interval;
    private long nIntervals;
    private Duration span;
    private Duration refreshRate;

    private Builder() {
      this.cache = new TreeMap<>();
      this.mergeStrategy = (oldValue, newValue) -> newValue;
      this.clock = Instant::now;
      this.interval = Duration.ofDays(1L);
      this.nIntervals = MIN_INTERVALS;
      this.refreshRate = Duration.ofMinutes(1L);
    }

    private Builder(IntervalCache<T> cache) {
      this.cache = new TreeMap<>(cache.cache);
      this.mergeStrategy = cache.mergeStrategy;
      this.clock = cache.clock;
      this.interval = cache.interval;
      this.nIntervals = cache.nIntervals;
      this.span = cache.span;
      this.refreshRate = cache.refreshRate;
    }

    /** Sets duration of each contiguous time interval. */
    public Builder<T> interval(Duration interval) {
      this.interval = interval;
      return this;
    }

    /** Sets the number of contiguous time intervals. */
    public Builder<T> nIntervals(long nIntervals) {
      this.nIntervals = nIntervals;
      return this;
    }

    /** Sets the duration after which the cache will refresh. */
    public Builder<T> refreshRate(Duration refreshRate) {
      this.refreshRate = refreshRate;
      return this;
    }

    /** Sets the clock that the cache will use for its notion of time. */
    public Builder<T> clock(Supplier<Instant> clock) {
      this.clock = clock;
      return this;
    }

    /** Sets the strategy that will be used when adding values to the cache. */
    public Builder<T> mergeStrategy(BinaryOperator<T> mergeStrategy) {
      this.mergeStrategy = mergeStrategy;
      return this;
    }

    /** Returns an initialized instance of the cache. */
    public IntervalCache<T> build() {
      checkFields();
      setSpan();
      initializeCache();
      return new IntervalCache<>(this);
    }

    private void checkFields() {
      Objects.requireNonNull(mergeStrategy);
      Objects.requireNonNull(clock);
      Objects.requireNonNull(interval);
      Objects.requireNonNull(refreshRate);
      checkArgument(!refreshRate.isNegative() && !refreshRate.isZero(), this::refreshRateMessage);
      checkArgument(nIntervals >= MIN_INTERVALS, this::nIntervalsMessage);
    }

    private String refreshRateMessage() {
      return "'refreshRate' must be positive; got " + refreshRate;
    }

    private String nIntervalsMessage() {
      return "'nIntervals' must be at least " + MIN_INTERVALS + "; got " + nIntervals;
    }

    private void setSpan() {
      span = interval.multipliedBy(nIntervals);
    }

    private void initializeCache() {
      if (cache.isEmpty()) {
        Instant now = clock.get();
        LongStream.range(0L, nIntervals)
            .mapToObj(i -> now.minus(interval.multipliedBy(i)))
            .forEach(timestamp -> cache.put(timestamp, null));
      }
    }
  }
}

package org.sharetrace.util;

import static org.sharetrace.util.Preconditions.checkArgument;
import static org.sharetrace.util.Preconditions.checkInClosedRange;
import static org.sharetrace.util.Preconditions.checkIsAtLeast;
import static org.sharetrace.util.Preconditions.checkIsPositive;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.SortedMap;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * A cache that lazily maintains a finite number of contiguous time intervals in which values are
 * cached. The timespan of the cache is defined as {@code nIntervals * intervalDuration}. This
 * implementation differs from a standard cache in that the time-to-live of a value is based on its
 * specified timestamp, and not how long it has existed in the cache.
 *
 * <p>Upon request to retrieve a value, the cache checks to see if it should synchronously refresh
 * itself by shifting forward its time horizon and removing expired values (based on their
 * timestamp). The frequency with which this refresh operation occurs is based on the specified
 * {@code refreshRate} and {@code clock}. Note that if all values have expired, the cache is
 * reinitialized based on the current time, according to the {@code clock}.
 *
 * <p>A user-defined strategy can be provided that determines how values in a given time interval
 * are merged. By default, the new value unconditionally replaces the old value.
 *
 * @param <T> The type of the cached values.
 */
public class IntervalCache<T> {

  private static final long MIN_INTERVALS = 1L;
  private static final long MIN_BUFFER = MIN_INTERVALS;
  private final SortedMap<Instant, T> cache;
  private final BinaryOperator<T> mergeStrategy;
  private final Supplier<Instant> clock;
  private final Duration interval;
  private final long nIntervals;
  private final long nBuffer;
  private final Duration refreshRate;
  private Instant lastRefresh;
  private Instant rangeStart;
  private Instant rangeEnd;

  private IntervalCache(Builder<T> builder) {
    cache = builder.cache;
    mergeStrategy = builder.mergeStrategy;
    clock = builder.clock;
    interval = builder.interval;
    nIntervals = builder.nIntervals;
    nBuffer = builder.nBuffer;
    refreshRate = builder.refreshRate;
    lastRefresh = clock.get();
    updateRange();
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * Returns the cached value associated with the time interval that contains the specified
   * timestamp. If no value has been cached in the time interval or the timestamp falls outside the
   * timespan of the cache, {@code null} is returned. Prior to retrieving the value, the cache is
   * possibly refreshed if it has been sufficiently long since its previous refresh.
   */
  @Nullable
  public T get(Instant timestamp) {
    Objects.requireNonNull(timestamp);
    refresh();
    return isInRange(timestamp) ? cache.get(floorKey(timestamp)) : null;
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
    Instant key = floorKey(checkInRange(timestamp));
    cache.merge(key, value, mergeStrategy);
  }

  /**
   * Returns the maximum value, according to the specified comparator, whose time interval ends
   * prior to specified timestamp. If no values have been cached in the time intervals that end
   * prior to the timestamp, {@code null} is returned. Prior to retrieving the value, the cache is
   * possibly refreshed if it has been sufficiently long since its previous refresh.
   */
  @Nullable
  public T headMax(Instant timestamp, Comparator<T> comparator) {
    Objects.requireNonNull(timestamp);
    Objects.requireNonNull(comparator);
    refresh();
    return cache.headMap(timestamp).values().stream().max(comparator).orElse(null);
  }

  private Instant floorKey(Instant timestamp) {
    Duration sinceStart = Duration.between(rangeStart, timestamp);
    long multiplier = (long) Math.floor(sinceStart.dividedBy(interval));
    return rangeStart.plus(interval.multipliedBy(multiplier));
  }

  private Instant checkInRange(Instant timestamp) {
    checkArgument(isInRange(timestamp), () -> rangeMessage(timestamp));
    return timestamp;
  }

  private boolean isInRange(Instant timestamp) {
    return rangeStart.isBefore(timestamp) && rangeEnd.isAfter(timestamp);
  }

  private String rangeMessage(Instant timestamp) {
    return "'timestamp' must be between " + rangeStart + " and " + rangeEnd + "; got " + timestamp;
  }

  private void refresh() {
    Duration sinceRefresh = Duration.between(lastRefresh, clock.get());
    if (sinceRefresh.compareTo(refreshRate) > 0) {
      updateRange();
      cache.headMap(rangeStart).clear();
      lastRefresh = clock.get();
    }
  }

  private void updateRange() {
    Instant now = clock.get();
    rangeStart = now.minus(interval.multipliedBy(nIntervals - nBuffer));
    rangeEnd = now.plus(interval.multipliedBy(nBuffer));
  }

  public static final class Builder<T> {

    private SortedMap<Instant, T> cache;
    private BinaryOperator<T> mergeStrategy;
    private Supplier<Instant> clock;
    private Duration interval;
    private long nIntervals = MIN_INTERVALS;
    private long nBuffer = MIN_BUFFER;
    private Duration refreshRate;
    private boolean prioritizeReads = false;

    /** Sets duration of each contiguous time interval. Default: 1 hour. */
    public Builder<T> interval(Duration interval) {
      this.interval = interval;
      return this;
    }

    /** Sets the total number of contiguous time intervals. Default: 1. */
    public Builder<T> nIntervals(long nIntervals) {
      this.nIntervals = nIntervals;
      return this;
    }

    /** Sets the number of "future" time intervals. Default: 1. */
    public Builder<T> nBuffer(long nBuffer) {
      this.nBuffer = nBuffer;
      return this;
    }

    /** Sets the duration after which the cache will refresh. Default: 1 minute. */
    public Builder<T> refreshRate(Duration refreshRate) {
      this.refreshRate = refreshRate;
      return this;
    }

    /** Sets the clock that the cache will use for its notion of time. Default: Instant::now. */
    public Builder<T> clock(Supplier<Instant> clock) {
      this.clock = clock;
      return this;
    }

    /**
     * Sets the strategy that will be used when adding values to the cache. Default: (oldValue,
     * newValue) -> newValue.
     */
    public Builder<T> mergeStrategy(BinaryOperator<T> mergeStrategy) {
      this.mergeStrategy = mergeStrategy;
      return this;
    }

    /** Sets whether read or write efficiency should be prioritized. Default: false. */
    public Builder<T> prioritizeReads(boolean prioritizeReads) {
      this.prioritizeReads = prioritizeReads;
      return this;
    }

    /** Returns an initialized instance of the cache. */
    public IntervalCache<T> build() {
      return new IntervalCache<>(checkFields());
    }

    private Builder<T> checkFields() {
      cache = newCache();
      mergeStrategy = Objects.requireNonNullElse(mergeStrategy, (oldValue, newValue) -> newValue);
      clock = Objects.requireNonNullElse(clock, Instant::now);
      interval = Objects.requireNonNullElse(interval, Duration.ofDays(1L));
      checkIsPositive(interval, "interval");
      checkIsAtLeast(nIntervals, MIN_INTERVALS, "nIntervals");
      checkInClosedRange(nBuffer, MIN_BUFFER, nIntervals, "nBuffer");
      refreshRate = Objects.requireNonNullElse(refreshRate, Duration.ofMinutes(1L));
      checkIsPositive(refreshRate, "refreshRate");
      return this;
    }

    private SortedMap<Instant, T> newCache() {
      return prioritizeReads ? new Object2ObjectAVLTreeMap<>() : new Object2ObjectRBTreeMap<>();
    }
  }
}

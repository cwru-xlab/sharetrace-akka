package org.sharetrace.util;

import static org.sharetrace.util.Preconditions.checkArgument;
import static org.sharetrace.util.Preconditions.checkIsAtLeast;
import static org.sharetrace.util.Preconditions.checkIsPositive;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
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
  private final SortedMap<Instant, T> cache;
  private final BinaryOperator<T> mergeStrategy;
  private final Supplier<Instant> clock;
  private final Duration interval;
  private final long nIntervals;
  private final Duration span;
  private final Duration refreshRate;
  private Instant lastRefresh;
  private Instant rangeStart;
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
   * prior to specified timestamp. If the timestamp falls outside the timespan of the cache or no
   * values have been cached in the time intervals that end prior to the timestamp, {@code null} is
   * returned. Prior to retrieving the value, the cache is possibly refreshed if it has been
   * sufficiently long since its previous refresh.
   */
  @Nullable
  public T headMax(Instant timestamp, Comparator<T> comparator) {
    Objects.requireNonNull(timestamp);
    Objects.requireNonNull(comparator);
    refresh();
    return timestamp.isBefore(rangeEnd)
        ? cache.headMap(timestamp).values().stream().max(comparator).orElse(null)
        : null;
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
      removeExpired();
      updateRange();
      lastRefresh = clock.get();
    }
  }

  private void removeExpired() {
    Instant expiredAt = clock.get().minus(span);
    cache.headMap(expiredAt).clear();
  }

  private void updateRange() {
    Instant startOfEnd = clock.get();
    rangeEnd = startOfEnd.plus(interval);
    rangeStart = startOfEnd.minus(interval.multipliedBy(nIntervals - 1));
  }

  public static final class Builder<T> {

    private final SortedMap<Instant, T> cache;
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
      return new IntervalCache<>(this);
    }

    private void checkFields() {
      Objects.requireNonNull(cache);
      Objects.requireNonNull(mergeStrategy);
      Objects.requireNonNull(clock);
      checkIsPositive(Objects.requireNonNull(interval), this::intervalMessage);
      checkIsAtLeast(nIntervals, MIN_INTERVALS, this::nIntervalsMessage);
      checkIsPositive(Objects.requireNonNull(refreshRate), this::refreshRateMessage);
    }

    private String refreshRateMessage() {
      return "'refreshRate' must be positive; got " + refreshRate;
    }

    private String intervalMessage() {
      return "'interval' must be positive; got " + interval;
    }

    private String nIntervalsMessage() {
      return "'nIntervals' must be at least " + MIN_INTERVALS + "; got " + nIntervals;
    }

    private void setSpan() {
      span = interval.multipliedBy(nIntervals);
    }
  }
}

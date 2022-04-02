package org.sharetrace.util;

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

public class IntervalCache<T> {

  private final NavigableMap<Instant, T> cache;
  private final BinaryOperator<T> mergeStrategy;
  private final Supplier<Instant> clock;
  private final Duration interval;
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
    this.span = builder.span;
    this.refreshRate = builder.refreshRate;
    this.lastRefresh = clock.get();
    updateRange();
  }

  public static <T> Builder<T> builder() {
    return new Builder<>();
  }

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

  public void put(Instant timestamp, T value) {
    Objects.requireNonNull(timestamp);
    Objects.requireNonNull(value);
    refresh();
    Instant key = cache.floorKey(checkInRange(timestamp));
    cache.merge(key, value, mergeStrategy);
  }

  public T headMax(Instant timestamp, Comparator<T> comparator) {
    Objects.requireNonNull(timestamp);
    Objects.requireNonNull(comparator);
    return cache.headMap(timestamp, true).values().stream()
        .filter(Objects::nonNull)
        .max(comparator)
        .orElse(null);
  }

  @Override
  public int hashCode() {
    return Objects.hash(cache, mergeStrategy, clock, interval, refreshRate);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntervalCache<?> that = (IntervalCache<?>) o;
    return cache.equals(that.cache)
        && mergeStrategy.equals(that.mergeStrategy)
        && clock.equals(that.clock)
        && interval.equals(that.interval)
        && refreshRate.equals(that.refreshRate);
  }

  @Override
  public String toString() {
    return "IntervalCache{"
        + "interval="
        + interval
        + ", refreshRate="
        + refreshRate
        + ", lastRefresh="
        + lastRefresh
        + ", rangeStart="
        + rangeStart
        + ", rangeEnd="
        + rangeEnd
        + '}';
  }

  private Instant checkInRange(Instant timestamp) {
    if (timestamp.isBefore(rangeStart) || timestamp.isAfter(rangeEnd)) {
      throw new IllegalArgumentException(
          "'timestamp' must be between " + rangeStart + " and " + rangeEnd + "; got " + timestamp);
    }
    return timestamp;
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
    LongStream.rangeClosed(1, nIntervals)
        .mapToObj(i -> startOfRangeEnd.plus(interval.multipliedBy(i)))
        .forEach(timestamp -> cache.put(timestamp, null));
  }

  private void updateRange() {
    rangeStart = cache.firstKey();
    startOfRangeEnd = cache.lastKey();
    rangeEnd = startOfRangeEnd.plus(interval);
  }

  public static final class Builder<T> {

    private NavigableMap<Instant, T> cache;
    private BinaryOperator<T> mergeStrategy;
    private Supplier<Instant> clock;
    private Duration interval;
    private long nIntervals;
    private Duration span;
    private Duration refreshRate;

    private Builder() {
      this.mergeStrategy = (oldValue, newValue) -> newValue;
      this.clock = Instant::now;
      this.interval = Duration.ofDays(1L);
      this.nIntervals = 1L;
      this.refreshRate = Duration.ofMinutes(1L);
    }

    public Builder<T> interval(Duration interval) {
      this.interval = interval;
      return this;
    }

    public Builder<T> nIntervals(long nIntervals) {
      this.nIntervals = nIntervals;
      return this;
    }

    public Builder<T> refreshRate(Duration refreshRate) {
      this.refreshRate = refreshRate;
      return this;
    }

    public Builder<T> clock(Supplier<Instant> clock) {
      this.clock = clock;
      return this;
    }

    public Builder<T> mergeStrategy(BinaryOperator<T> mergeStrategy) {
      this.mergeStrategy = mergeStrategy;
      return this;
    }

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
      if (refreshRate.isNegative() || refreshRate.isZero()) {
        throw new IllegalArgumentException("'refreshRate' must be positive; got " + refreshRate);
      }
      if (nIntervals < 1L) {
        throw new IllegalArgumentException("'nIntervals' must be at least 1; got " + nIntervals);
      }
    }

    private void setSpan() {
      span = interval.multipliedBy(nIntervals);
    }

    private void initializeCache() {
      cache = new TreeMap<>();
      Instant now = clock.get();
      LongStream.range(0L, nIntervals)
          .mapToObj(i -> now.minus(interval.multipliedBy(i)))
          .forEach(timestamp -> cache.put(timestamp, null));
    }
  }
}

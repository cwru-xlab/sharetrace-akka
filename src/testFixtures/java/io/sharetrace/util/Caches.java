package io.sharetrace.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class Caches {

  private static final Duration INTERVAL = Duration.ofDays(1L);
  private static final Clock NOW_FIXED = Clocks.nowFixed();

  private Caches() {}

  public static IntervalCache<Integer> empty() {
    return empty(Clocks.nowFixed());
  }

  public static IntervalCache<Integer> empty(Clock clock) {
    return IntervalCache.create(
        CacheParams.<Integer>builder()
            .clock(clock)
            .interval(INTERVAL)
            .numIntervals(1)
            .refreshPeriod(Duration.ofMinutes(1L))
            .numLookAhead(0)
            .mergeStrategy(Integer::max)
            .build());
  }

  public static Duration interval() {
    return INTERVAL;
  }

  public static IntervalCache<Integer> withEntry() {
    return withEntry(NOW_FIXED);
  }

  public static IntervalCache<Integer> withEntry(Clock clock) {
    IntervalCache<Integer> cache = empty(clock);
    cache.put(inRange(), cached());
    return cache;
  }

  public static Instant inRange() {
    return NOW_FIXED.instant().minusSeconds(10L);
  }

  public static Instant atUpperBound() {
    return NOW_FIXED.instant();
  }

  public static Instant atLowerBound() {
    return NOW_FIXED.instant().minus(INTERVAL);
  }

  public static Instant belowLowerBound() {
    return NOW_FIXED.instant().minus(INTERVAL.plusSeconds(1L));
  }

  public static int cached() {
    return 1;
  }
}

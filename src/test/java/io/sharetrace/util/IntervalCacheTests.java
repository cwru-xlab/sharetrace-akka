package io.sharetrace.util;

import io.sharetrace.util.Clocks.MutableClock;
import io.sharetrace.util.cache.IntervalCache;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// TODO Fix when isNotAfter uses strict less than
public class IntervalCacheTests {

  @Test
  public void getFromEmptyReturnsEmpty() {
    Assertions.assertEquals(Caches.empty().get(Clocks.nowFixed().instant()), Optional.empty());
  }

  @Test
  public void getEntryInRangeReturnsValue() {
    Optional<Integer> cached = Caches.withEntry().get(Caches.inRange());
    Assertions.assertTrue(cached.isPresent());
    Assertions.assertEquals(cached.get(), Caches.cached());
  }

  @Test
  public void getMergedEntryInRangeReturnsValue() {
    IntervalCache<Integer> cache = Caches.withEntry();
    int newValue = Caches.cached() + 1;
    cache.put(Caches.inRange(), newValue);
    Optional<Integer> cached = cache.get(Caches.inRange());
    Assertions.assertTrue(cached.isPresent());
    Assertions.assertEquals(cached.get(), newValue);
  }

  @Test
  public void getEntryAtUpperBoundReturnsEmpty() {
    Assertions.assertEquals(Caches.withEntry().get(Caches.atUpperBound()), Optional.empty());
  }

  @Test
  public void getEntryAtLowerBoundReturnsValue() {
    Optional<Integer> cached = Caches.withEntry().get(Caches.atLowerBound());
    Assertions.assertTrue(cached.isPresent());
    Assertions.assertEquals(cached.get(), Caches.cached());
  }

  @Test
  public void getEntryBelowLowerBoundReturnsEmpty() {
    Assertions.assertEquals(Caches.withEntry().get(Caches.belowLowerBound()), Optional.empty());
  }

  @Test
  public void putEntryAtUpperBoundThrowsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> Caches.empty().put(Caches.atUpperBound(), Caches.cached()));
  }

  @Test
  public void putEntryAtLowerBoundDoesNotThrowException() {
    Assertions.assertDoesNotThrow(() -> Caches.empty().put(Caches.atLowerBound(), Caches.cached()));
  }

  @Test
  public void putEntryBelowLowerBoundThrowsException() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> Caches.empty().put(Caches.belowLowerBound(), Caches.cached()));
  }

  @Test
  public void maxInRangeReturnsMaxCachedValue() {
    IntervalCache<Integer> cache = Caches.withEntry();
    int maxValue = Caches.cached() + 1;
    cache.put(Caches.inRange(), maxValue);
    Optional<Integer> cached = cache.max(Caches.inRange());
    Assertions.assertTrue(cached.isPresent());
    Assertions.assertEquals(cached.get(), maxValue);
  }

  @Test
  public void maxAtLowerBoundReturnsMaxCachedValue() {
    Optional<Integer> max = Caches.withEntry().max(Caches.atLowerBound());
    Assertions.assertTrue(max.isPresent());
    Assertions.assertEquals(max.get(), Caches.cached());
  }

  @Test
  public void maxBelowBoundReturnsEmpty() {
    Assertions.assertEquals(Caches.withEntry().max(Caches.belowLowerBound()), Optional.empty());
  }

  @Test
  public void maxAtUpperBoundReturnsMaxCachedValue() {
    Optional<Integer> max = Caches.withEntry().max(Caches.atUpperBound());
    Assertions.assertTrue(max.isPresent());
    Assertions.assertEquals(max.get(), Caches.cached());
  }

  @Test
  public void getAfterTimeShiftReturnsEmpty() {
    MutableClock clock = Clocks.mutableNowFixed();
    IntervalCache<Integer> cache = Caches.withEntry(clock);
    clock.tick(Caches.interval());
    Assertions.assertEquals(cache.get(Caches.inRange()), Optional.empty());
  }
}

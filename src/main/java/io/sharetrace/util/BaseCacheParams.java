package io.sharetrace.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.function.BinaryOperator;
import org.immutables.value.Value;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseCacheParams<T> {

  public abstract int numIntervals();

  public abstract int numLookAhead();

  public abstract Duration interval();

  public abstract Duration refreshPeriod();

  @JsonIgnore
  public abstract BinaryOperator<T> mergeStrategy();

  @JsonIgnore
  public abstract Clock clock();

  @JsonIgnore
  public abstract Comparator<T> comparator();

  @Value.Check
  protected void checkFields() {
    Checks.isAtLeast(interval(), Duration.ZERO, "interval");
    Checks.isAtLeast(numIntervals(), 1, "numIntervals");
    Checks.inClosedOpen(numLookAhead(), 0, numIntervals(), "numLookAhead");
    Checks.isAtLeast(refreshPeriod(), Duration.ZERO, "refreshPeriod");
  }
}

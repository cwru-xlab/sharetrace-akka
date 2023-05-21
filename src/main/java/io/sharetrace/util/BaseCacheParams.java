package io.sharetrace.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Range;
import java.time.Clock;
import java.time.Duration;
import java.util.function.BinaryOperator;
import org.immutables.value.Value;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseCacheParams<T> {

  public static final int MIN_INTERVALS = 1;
  public static final int MIN_LOOK_AHEAD = 0;
  public static final Duration MIN_INTERVAL = Duration.ZERO;
  public static final Duration MIN_REFRESH_PERIOD = Duration.ZERO;

  private static final Range<Duration> INTERVAL_RANGE = Range.atLeast(MIN_INTERVAL);
  private static final Range<Integer> INTERVALS_RANGE = Range.atLeast(MIN_INTERVALS);
  private static final Range<Duration> REFRESH_PERIOD_RANGE = Range.atLeast(MIN_REFRESH_PERIOD);

  @JsonIgnore
  public abstract BinaryOperator<T> mergeStrategy();

  @JsonIgnore
  public abstract Clock clock();

  @Value.Check
  protected void checkFields() {
    Checks.checkRange(interval(), INTERVAL_RANGE, "interval");
    Checks.checkRange(numIntervals(), INTERVALS_RANGE, "numIntervals");
    Checks.checkRange(numLookAhead(), lookAheadRange(), "numLookAhead");
    Checks.checkRange(refreshPeriod(), REFRESH_PERIOD_RANGE, "refreshPeriod");
  }

  public abstract Duration interval();

  public abstract int numIntervals();

  public abstract int numLookAhead();

  private Range<Integer> lookAheadRange() {
    return Range.closedOpen(MIN_LOOK_AHEAD, numIntervals());
  }

  public abstract Duration refreshPeriod();
}

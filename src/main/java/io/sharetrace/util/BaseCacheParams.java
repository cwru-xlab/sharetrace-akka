package io.sharetrace.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

  private static final String INTERVAL = "interval";
  private static final String NUM_INTERVALS = "numIntervals";
  private static final String NUM_LOOK_AHEAD = "numLookAhead";
  private static final String REFRESH_PERIOD = "refreshPeriod";

  @JsonIgnore
  public abstract BinaryOperator<T> mergeStrategy();

  @JsonIgnore
  public abstract Clock clock();

  @Value.Check
  protected void checkFields() {
    Checks.isAtLeast(interval(), MIN_INTERVAL, INTERVAL);
    Checks.isAtLeast(numIntervals(), MIN_INTERVALS, NUM_INTERVALS);
    Checks.inClosedOpen(numLookAhead(), MIN_LOOK_AHEAD, numIntervals(), NUM_LOOK_AHEAD);
    Checks.isAtLeast(refreshPeriod(), MIN_REFRESH_PERIOD, REFRESH_PERIOD);
  }

  public abstract Duration interval();

  public abstract int numIntervals();

  public abstract int numLookAhead();

  public abstract Duration refreshPeriod();
}

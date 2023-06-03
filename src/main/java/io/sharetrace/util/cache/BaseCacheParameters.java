package io.sharetrace.util.cache;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.google.common.collect.Range;
import io.sharetrace.util.Checks;
import io.sharetrace.util.logging.ToClassNameSerializer;
import java.time.Clock;
import java.time.Duration;
import java.util.function.BinaryOperator;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseCacheParameters<V extends Comparable<? super V>> {

  public static final long MIN_INTERVALS = 1;
  public static final long MIN_FORWARD_INTERVALS = 0;
  public static final Duration MIN_INTERVAL = Duration.ZERO;
  public static final Duration MIN_REFRESH_PERIOD = Duration.ZERO;

  private static final Range<Duration> INTERVAL_RANGE = Range.atLeast(MIN_INTERVAL);
  private static final Range<Long> INTERVALS_RANGE = Range.atLeast(MIN_INTERVALS);
  private static final Range<Duration> REFRESH_PERIOD_RANGE = Range.atLeast(MIN_REFRESH_PERIOD);

  @JsonSerialize(using = ToClassNameSerializer.class)
  public abstract BinaryOperator<V> mergeStrategy();

  @JsonSerialize(using = ToStringSerializer.class)
  public abstract Clock clock();

  public abstract Duration interval();

  public abstract long intervals();

  public abstract long forwardIntervals();

  public abstract Duration refreshPeriod();

  @Value.Check
  protected void check() {
    Checks.checkRange(interval(), INTERVAL_RANGE, "interval");
    Checks.checkRange(intervals(), INTERVALS_RANGE, "intervals");
    Checks.checkRange(forwardIntervals(), forwardRange(), "forwardIntervals");
    Checks.checkRange(refreshPeriod(), REFRESH_PERIOD_RANGE, "refreshPeriod");
  }

  private Range<Long> forwardRange() {
    return Range.closedOpen(MIN_FORWARD_INTERVALS, intervals());
  }
}

package io.sharetrace.util.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Range;
import io.sharetrace.util.Checks;
import java.time.Clock;
import java.time.Duration;
import java.util.function.BinaryOperator;
import org.immutables.value.Value;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseCacheParameters<V extends Comparable<? super V>> {

  public static final long MIN_INTERVALS = 1;
  public static final long MIN_LOOK_AHEAD = 0;
  public static final Duration MIN_INTERVAL = Duration.ZERO;
  public static final Duration MIN_REFRESH_PERIOD = Duration.ZERO;

  private static final Range<Duration> INTERVAL_RANGE = Range.atLeast(MIN_INTERVAL);
  private static final Range<Long> INTERVALS_RANGE = Range.atLeast(MIN_INTERVALS);
  private static final Range<Duration> REFRESH_PERIOD_RANGE = Range.atLeast(MIN_REFRESH_PERIOD);

  @JsonIgnore
  public abstract BinaryOperator<V> mergeStrategy();

  @JsonIgnore
  public abstract Clock clock();

  public abstract Duration interval();

  public abstract long intervals();

  public abstract long lookAhead();

  public abstract Duration refreshPeriod();

  @Value.Check
  protected void checkFields() {
    Checks.checkRange(interval(), INTERVAL_RANGE, "interval");
    Checks.checkRange(intervals(), INTERVALS_RANGE, "intervals");
    Checks.checkRange(lookAhead(), Range.closedOpen(MIN_LOOK_AHEAD, intervals()), "lookAhead");
    Checks.checkRange(refreshPeriod(), REFRESH_PERIOD_RANGE, "refreshPeriod");
  }
}

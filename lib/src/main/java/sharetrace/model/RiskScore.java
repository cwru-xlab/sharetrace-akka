package sharetrace.model;

import com.google.common.collect.Range;
import java.time.Duration;
import java.time.Instant;
import java.util.function.DoubleUnaryOperator;
import sharetrace.util.Ranges;

public record RiskScore(double value, Instant timestamp, Instant expiryTime)
    implements TemporalScore {

  private static final Range<Double> VALUE_RANGE = Range.closed(0d, 1d);
  private static final Range<Instant> TIME_RANGE = Range.atLeast(Timestamped.MIN_TIME);

  public static final double MIN_VALUE = VALUE_RANGE.lowerEndpoint();
  public static final double MAX_VALUE = VALUE_RANGE.upperEndpoint();
  public static final Instant MIN_TIME = TIME_RANGE.lowerEndpoint();
  public static final RiskScore MIN = new RiskScore(MIN_VALUE, MIN_TIME, MIN_TIME);

  public RiskScore {
    Ranges.check("value", value, VALUE_RANGE);
    Ranges.check("timestamp", timestamp, TIME_RANGE);
    Ranges.check("expiryTime", expiryTime, TIME_RANGE);
  }

  public RiskScore(double value, Instant timestamp, Duration expiry) {
    this(value, timestamp, timestamp.plus(expiry));
  }

  public RiskScore mapValue(DoubleUnaryOperator mapper) {
    return new RiskScore(mapper.applyAsDouble(value), timestamp, expiryTime);
  }
}

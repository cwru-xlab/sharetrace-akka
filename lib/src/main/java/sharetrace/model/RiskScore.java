package sharetrace.model;

import com.google.common.collect.Range;
import java.time.Duration;
import java.util.function.DoubleUnaryOperator;
import sharetrace.util.Ranges;

public record RiskScore(double value, Timestamp timestamp, Timestamp expiryTime)
    implements TemporalScore {

  private static final Range<Double> VALUE_RANGE = Range.closed(0d, 1d);

  public static final double MIN_VALUE = VALUE_RANGE.lowerEndpoint();
  public static final double MAX_VALUE = VALUE_RANGE.upperEndpoint();
  public static final RiskScore MIN = new RiskScore(MIN_VALUE, Timestamp.MIN, Timestamp.MIN);

  public RiskScore {
    Ranges.check("value", value, VALUE_RANGE);
  }

  public RiskScore(double value, Timestamp timestamp, Duration expiry) {
    this(value, timestamp, timestamp.plus(expiry));
  }

  public RiskScore mapValue(DoubleUnaryOperator mapper) {
    return new RiskScore(mapper.applyAsDouble(value), timestamp, expiryTime);
  }
}

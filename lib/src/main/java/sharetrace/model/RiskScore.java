package sharetrace.model;

import java.util.function.DoubleUnaryOperator;

public record RiskScore(double value, long timestamp, long expiryTime) implements TemporalScore {

  public static final double MIN_VALUE = 0d;
  public static final double MAX_VALUE = 1d;
  public static final RiskScore MIN = new RiskScore(MIN_VALUE, 0L, 0L);

  public static RiskScore fromExpiry(double value, long timestamp, long expiry) {
    return new RiskScore(value, timestamp, Math.addExact(timestamp, expiry));
  }

  public RiskScore mapValue(DoubleUnaryOperator mapper) {
    return new RiskScore(mapper.applyAsDouble(value), timestamp, expiryTime);
  }
}

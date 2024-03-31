package sharetrace.model;

import java.util.function.DoubleUnaryOperator;

public record RiskScore(double value, long timestamp, long expiryTime) implements TemporalScore {

  public static final RiskScore MIN = new RiskScore(0, 0, 0);

  public static RiskScore fromExpiry(double value, long timestamp, long expiry) {
    return new RiskScore(value, timestamp, Math.addExact(timestamp, expiry));
  }

  public RiskScore mapValue(DoubleUnaryOperator mapper) {
    return new RiskScore(mapper.applyAsDouble(value), timestamp, expiryTime);
  }
}

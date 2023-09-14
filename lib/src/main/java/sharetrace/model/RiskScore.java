package sharetrace.model;

import java.time.Duration;
import java.time.Instant;
import java.util.function.DoubleUnaryOperator;

public record RiskScore(double value, Instant timestamp, Instant expiresAt)
    implements TemporalScore {

  public static final double MIN_VALUE = 0;
  public static final double MAX_VALUE = 1;
  public static final RiskScore MIN = new RiskScore(MIN_VALUE, Instant.EPOCH, Instant.EPOCH);

  public RiskScore(double value, Instant timestamp, Duration expiry) {
    this(value, timestamp, timestamp.plus(expiry));
  }

  public RiskScore mapValue(DoubleUnaryOperator mapper) {
    return new RiskScore(mapper.applyAsDouble(value), timestamp, expiresAt);
  }
}

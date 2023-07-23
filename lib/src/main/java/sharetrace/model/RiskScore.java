package sharetrace.model;

import com.google.common.collect.Range;
import java.time.Duration;
import java.time.Instant;
import java.util.function.UnaryOperator;
import sharetrace.util.Checks;

public record RiskScore(float value, Instant timestamp, Instant expiresAt)
    implements TemporalScore {

  public static final float MIN_VALUE = 0;
  public static final float MAX_VALUE = 1;
  public static final RiskScore MIN = new RiskScore(MIN_VALUE, Instant.MIN, Instant.MIN);

  public RiskScore {
    Checks.checkRange(value, Range.closed(MIN_VALUE, MAX_VALUE), "value");
  }

  public RiskScore(float value, Instant timestamp, Duration expiry) {
    this(value, timestamp, timestamp.plus(expiry));
  }

  public RiskScore mapValue(UnaryOperator<Float> mapper) {
    return new RiskScore(mapper.apply(value), timestamp, expiresAt);
  }
}

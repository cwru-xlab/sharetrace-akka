package sharetrace.model;

import com.google.common.collect.Range;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import java.time.Duration;
import java.time.Instant;
import sharetrace.util.Checks;

public record RiskScore(float value, Instant timestamp, Instant expiresAt)
    implements TemporalScore {

  public static final float MIN_VALUE = 0;
  public static final float MAX_VALUE = 1;
  public static final RiskScore MIN = new RiskScore(MIN_VALUE, Instant.EPOCH, Instant.EPOCH);

  public RiskScore {
    Checks.checkRange(value, Range.closed(MIN_VALUE, MAX_VALUE), "value");
  }

  public RiskScore(float value, Instant timestamp, Duration expiry) {
    this(value, timestamp, timestamp.plus(expiry));
  }

  public RiskScore mapValue(FloatUnaryOperator mapper) {
    return new RiskScore(mapper.apply(value), timestamp, expiresAt);
  }
}

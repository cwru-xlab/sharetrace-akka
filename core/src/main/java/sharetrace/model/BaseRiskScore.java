package sharetrace.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Range;
import java.time.Duration;
import java.time.Instant;
import org.immutables.value.Value;
import sharetrace.util.Checks;

@Value.Immutable
@SuppressWarnings("StaticInitializerReferencesSubClass")
abstract class BaseRiskScore implements TemporalScore {

  public static final float MIN_VALUE = 0;
  public static final float MAX_VALUE = 1;
  public static final float RANGE = MAX_VALUE - MIN_VALUE;
  public static final Instant MIN_TIMESTAMP = Instant.EPOCH;
  public static final Duration MIN_EXPIRY = Duration.ZERO;
  public static final RiskScore MIN = RiskScore.of(MIN_VALUE, MIN_TIMESTAMP, MIN_EXPIRY);

  @Override
  @Value.Parameter
  public abstract float value();

  @Override
  @Value.Parameter
  public abstract Instant timestamp();

  @Override
  @JsonIgnore
  @Value.Parameter
  public abstract Duration expiry();

  @Value.Check
  protected void check() {
    Checks.checkRange(value(), Range.closed(MIN_VALUE, MAX_VALUE), "value");
    Checks.checkRange(timestamp(), Range.atLeast(MIN_TIMESTAMP), "timestamp");
    Checks.checkRange(expiry(), Range.atLeast(MIN_EXPIRY), "expiry");
  }
}

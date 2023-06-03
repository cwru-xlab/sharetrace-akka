package io.sharetrace.model;

import com.google.common.collect.Range;
import io.sharetrace.util.Checks;
import java.time.Instant;
import org.immutables.value.Value;

@Value.Immutable
@SuppressWarnings("StaticInitializerReferencesSubClass")
abstract class BaseRiskScore implements TemporalScore {

  public static final float MIN_VALUE = 0;
  public static final float MAX_VALUE = 1;
  public static final float RANGE = MAX_VALUE - MIN_VALUE;
  public static final Instant MIN_TIMESTAMP = Instant.EPOCH;

  private static final Range<Float> VALUE_RANGE = Range.closed(MIN_VALUE, MAX_VALUE);
  private static final Range<Instant> TIMESTAMP_RANGE = Range.atLeast(MIN_TIMESTAMP);

  public static final RiskScore MIN = RiskScore.of(MIN_VALUE, MIN_TIMESTAMP);

  @Override
  @Value.Parameter
  public abstract float value();

  @Override
  @Value.Parameter
  public abstract Instant timestamp();

  @Value.Check
  protected void check() {
    Checks.checkRange(value(), VALUE_RANGE, "value");
    Checks.checkRange(timestamp(), TIMESTAMP_RANGE, "timestamp");
  }
}

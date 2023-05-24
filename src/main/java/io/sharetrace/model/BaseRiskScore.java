package io.sharetrace.model;

import com.google.common.collect.Range;
import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.actor.UserActor;
import io.sharetrace.model.message.RiskScoreMessage;
import io.sharetrace.util.Checks;
import java.time.Instant;
import org.immutables.value.Value;

/**
 * A timestamped infection probability of a user. A risk score derived only from user symptoms, it
 * is called a <i>symptom score</i>. A risk score that also accounts for direct or indirect contact
 * that a user had with others is called an <i>exposure score</i>.
 *
 * @see RiskScoreMessage
 * @see UserActor
 * @see RiskPropagation
 */
@SuppressWarnings({"DefaultAnnotationParam", "StaticInitializerReferencesSubClass"})
@Value.Immutable(copy = true)
abstract class BaseRiskScore implements TemporalScore {

  public static final float MIN_VALUE = 0f;
  public static final float MAX_VALUE = 1f;
  public static final float RANGE = MAX_VALUE - MIN_VALUE;
  public static final Instant MIN_TIME = Instant.EPOCH;

  private static final Range<Float> VALUE_RANGE = Range.closed(MIN_VALUE, MAX_VALUE);
  private static final Range<Instant> TIME_RANGE = Range.atLeast(MIN_TIME);

  public static final RiskScore MIN = RiskScore.of(MIN_VALUE, MIN_TIME);

  /** Returns the probability of infection. */
  @Override
  @Value.Parameter
  public abstract float value();

  /** Returns when this probability was first computed. */
  @Override
  @Value.Parameter
  public abstract Instant timestamp();

  @Value.Check
  protected void check() {
    Checks.checkRange(value(), VALUE_RANGE, "value");
    Checks.checkRange(timestamp(), TIME_RANGE, "time");
  }
}

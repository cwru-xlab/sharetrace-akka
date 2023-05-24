package io.sharetrace.model;

import com.google.common.collect.Range;
import io.sharetrace.util.Checks;
import java.time.Duration;
import org.immutables.value.Value;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseUserParameters {

  public static final float MIN_TRANSMISSION_RATE = 0;
  public static final float MAX_TRANSMISSION_RATE = 1;
  public static final float MIN_SEND_COEFFICIENT = 0;
  public static final float MIN_TOLERANCE = 0;
  public static final Duration MIN_TIME_BUFFER = Duration.ZERO;
  public static final Duration MIN_SCORE_TIME_TO_LIVE = Duration.ZERO;
  public static final Duration MIN_CONTACT_TIME_TO_LIVE = Duration.ZERO;
  public static final Duration MIN_IDLE_TIMEOUT = Duration.ZERO;

  private static final Range<Float> TRANSMISSION_RATE_RANGE =
      Range.open(MIN_TRANSMISSION_RATE, MAX_TRANSMISSION_RATE);
  private static final Range<Float> SEND_COEFFIENT_RANGE = Range.atLeast(MIN_SEND_COEFFICIENT);
  private static final Range<Duration> TIME_BUFFER_RANGE = Range.atLeast(MIN_TIME_BUFFER);
  private static final Range<Duration> SCORE_TIME_TO_LIVE_RANGE =
      Range.greaterThan(MIN_SCORE_TIME_TO_LIVE);
  private static final Range<Duration> CONTACT_TIME_TO_LIVE_RANGE =
      Range.greaterThan(MIN_CONTACT_TIME_TO_LIVE);
  private static final Range<Float> TOLERANCE_RANGE = Range.atLeast(MIN_TOLERANCE);
  private static final Range<Duration> IDLE_TIMEOUT_RANGE = Range.greaterThan(MIN_IDLE_TIMEOUT);

  @Value.Check
  protected void check() {
    Checks.checkRange(transmissionRate(), TRANSMISSION_RATE_RANGE, "transmissionRate");
    Checks.checkRange(sendCoefficient(), SEND_COEFFIENT_RANGE, "sendCoefficient");
    Checks.checkRange(timeBuffer(), TIME_BUFFER_RANGE, "timeBuffer");
    Checks.checkRange(scoreTimeToLive(), SCORE_TIME_TO_LIVE_RANGE, "scoreTimeToLive");
    Checks.checkRange(contactTimeToLive(), CONTACT_TIME_TO_LIVE_RANGE, "contactTimeToLive");
    Checks.checkRange(tolerance(), TOLERANCE_RANGE, "tolerance");
    Checks.checkRange(idleTimeout(), IDLE_TIMEOUT_RANGE, "idleTimeout");
  }

  public abstract float transmissionRate();

  public abstract float sendCoefficient();

  public abstract Duration timeBuffer();

  public abstract Duration scoreTimeToLive();

  public abstract Duration contactTimeToLive();

  public abstract float tolerance();

  public abstract Duration idleTimeout();
}

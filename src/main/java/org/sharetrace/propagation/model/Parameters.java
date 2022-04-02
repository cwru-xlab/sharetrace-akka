package org.sharetrace.propagation.model;

import java.time.Duration;
import org.immutables.value.Value;

@Value.Immutable
public abstract class Parameters implements NodeMessage {

  public static final double MIN_TRANSMISSION_RATE = 0d;
  public static final double MAX_TRANSMISSION_RATE = 1d;
  public static final double MIN_SEND_TOLERANCE = 0d;
  private static final String TR_MESSAGE =
      "'transmissionRate' must be between "
          + MIN_TRANSMISSION_RATE
          + " and "
          + MAX_TRANSMISSION_RATE
          + ", "
          + "inclusive; got %s";
  private static final String SEND_TOLERANCE_MESSAGE =
      "'sendTolerance' must be at least " + MIN_SEND_TOLERANCE + "; got %s";
  private static final String TIME_BUFFER_MESSAGE = "'timeBuffer' must be positive; got %s";
  private static final String SCORE_TTL_MESSAGE = "'scoreTtl' must be positive; got %s";

  public static Builder builder() {
    return ImmutableParameters.builder();
  }

  private static <T> void checkArgument(boolean expression, String message, T value) {
    if (!expression) {
      throw new IllegalArgumentException(String.format(message, value));
    }
  }

  public abstract double sendTolerance();

  public abstract double transmissionRate();

  public abstract Duration timeBuffer();

  public abstract Duration scoreTtl();

  @Value.Check
  protected void check() {
    double rate = transmissionRate();
    double tolerance = sendTolerance();
    Duration buffer = timeBuffer();
    Duration scoreTtl = scoreTtl();
    checkArgument(rate >= MIN_TRANSMISSION_RATE || rate <= MAX_TRANSMISSION_RATE, TR_MESSAGE, rate);
    checkArgument(tolerance >= MIN_SEND_TOLERANCE, SEND_TOLERANCE_MESSAGE, tolerance);
    checkArgument(!buffer.isZero() && !buffer.isNegative(), TIME_BUFFER_MESSAGE, buffer);
    checkArgument(!scoreTtl.isZero() && !scoreTtl.isNegative(), SCORE_TTL_MESSAGE, scoreTtl);
  }

  public interface Builder {

    Builder sendTolerance(double sendTolerance);

    Builder transmissionRate(double transmissionRate);

    Builder timeBuffer(Duration timeBuffer);

    Builder scoreTtl(Duration scoreTtl);

    Parameters build();
  }
}

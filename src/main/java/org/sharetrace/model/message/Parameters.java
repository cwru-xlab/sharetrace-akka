package org.sharetrace.model.message;

import java.time.Duration;
import org.immutables.value.Value;
import org.sharetrace.util.Preconditions;

@Value.Immutable
public abstract class Parameters implements NodeMessage {

  public static final double MIN_TRANSMISSION_RATE = 0d;
  public static final double MAX_TRANSMISSION_RATE = 1d;
  public static final double MIN_SEND_TOLERANCE = 0d;

  public static Builder builder() {
    return ImmutableParameters.builder();
  }

  public abstract double sendTolerance();

  public abstract double transmissionRate();

  public abstract Duration timeBuffer();

  public abstract Duration scoreTtl();

  @Value.Check
  protected void check() {
    Preconditions.checkArgument(
        transmissionRate() >= MIN_TRANSMISSION_RATE || transmissionRate() <= MAX_TRANSMISSION_RATE,
        transmissionRateMessage());
    Preconditions.checkArgument(sendTolerance() >= MIN_SEND_TOLERANCE, sendToleranceMessage());
    Preconditions.checkArgument(!timeBuffer().isZero() && !timeBuffer().isNegative(),
        timeBufferMessage());
    Preconditions.checkArgument(!scoreTtl().isZero() && !scoreTtl().isNegative(),
        scoreTtlMessage());
  }

  private String transmissionRateMessage() {
    return "'transmissionRate' must be between "
        + MIN_TRANSMISSION_RATE
        + " and "
        + MAX_TRANSMISSION_RATE
        + ", inclusive; got "
        + transmissionRate();
  }

  private String sendToleranceMessage() {
    return "'sendTolerance' must be at least " + MIN_SEND_TOLERANCE + "; got " + sendTolerance();
  }

  private String timeBufferMessage() {
    return "'timeBuffer' must be positive; got " + timeBuffer();
  }

  private String scoreTtlMessage() {
    return "'scoreTtl' must be positive; got " + scoreTtl();
  }

  public interface Builder {

    Builder sendTolerance(double sendTolerance);

    Builder transmissionRate(double transmissionRate);

    Builder timeBuffer(Duration timeBuffer);

    Builder scoreTtl(Duration scoreTtl);

    Parameters build();
  }
}

package sharetrace.model;

import com.google.common.collect.Range;
import java.time.Duration;
import sharetrace.Buildable;

@Buildable
public record Parameters(
    double transmissionRate,
    double sendCoefficient,
    double tolerance,
    long timeBuffer,
    long scoreExpiry,
    long contactExpiry,
    Duration flushTimeout,
    Duration idleTimeout) {

  public Parameters {
    // Greater than 0 to avoid divide-by-zero errors; less than 1 to ensure finite runtime.
    Ranges.check("transmissionRate", transmissionRate, Range.open(0d, 1d));
    Ranges.check("sendCoefficient", sendCoefficient, Range.atLeast(0d));
    Ranges.check("tolerance", tolerance, Range.atLeast(0d));
    Ranges.check("timeBuffer", timeBuffer, Range.atLeast(0L));
    Ranges.check("scoreExpiry", scoreExpiry, Range.atLeast(1L));
    Ranges.check("contactExpiry", contactExpiry, Range.atLeast(1L));
    // 1 millisecond is the minimum duration supported by Akka for scheduled messages.
    Ranges.check("flushTimeout", flushTimeout, Range.atLeast(Duration.ofMillis(1L)));
    Ranges.check("idleTimeout", idleTimeout, Range.atLeast(Duration.ofMillis(1L)));
  }
}

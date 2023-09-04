package sharetrace.model;

import com.google.common.collect.Range;
import java.time.Duration;
import sharetrace.Buildable;
import sharetrace.util.Ranges;

@Buildable
public record Parameters(
    double transmissionRate,
    double sendCoefficient,
    Duration timeBuffer,
    Duration scoreExpiry,
    Duration contactExpiry,
    Duration idleTimeout) {

  public Parameters {
    // Greater than 0 to avoid divide-by-zero errors; less than 1 to ensure finite runtime.
    Ranges.check("transmissionRate", transmissionRate, Range.open(0d, 1d));
    Ranges.check("sendCoefficient", sendCoefficient, Range.atLeast(0d));
    Ranges.check("timeBuffer", timeBuffer, Range.atLeast(Duration.ZERO));
    Ranges.check("scoreExpiry", scoreExpiry, Range.greaterThan(Duration.ZERO));
    Ranges.check("contactExpiry", contactExpiry, Range.greaterThan(Duration.ZERO));
    // 1 millisecond is the minimum duration supported by Akka for scheduled messages.
    Ranges.check("idleTimeout", idleTimeout, Range.atLeast(Duration.ofMillis(1)));
  }
}

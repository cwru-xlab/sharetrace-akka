package sharetrace.model;

import com.google.common.collect.Range;
import sharetrace.Buildable;
import sharetrace.util.Ranges;

@Buildable
public record Parameters(
    double transmissionRate,
    double sendCoefficient,
    long timeBuffer,
    long scoreExpiry,
    long contactExpiry,
    long idleTimeout) {

  public Parameters {
    // Greater than 0 to avoid divide-by-zero errors; less than 1 to ensure finite runtime.
    Ranges.check("transmissionRate", transmissionRate, Range.open(0d, 1d));
    Ranges.check("sendCoefficient", sendCoefficient, Range.atLeast(0d));
    Ranges.check("timeBuffer", timeBuffer, Range.atLeast(0L));
    Ranges.check("scoreExpiry", scoreExpiry, Range.greaterThan(0L));
    Ranges.check("contactExpiry", contactExpiry, Range.greaterThan(0L));
    // 1 millisecond is the minimum duration supported by Akka for scheduled messages.
    Ranges.check("idleTimeout", idleTimeout, Range.atLeast(1L));
  }
}

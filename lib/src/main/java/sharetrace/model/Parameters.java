package sharetrace.model;

import com.google.common.collect.Range;
import java.time.Duration;
import sharetrace.Buildable;
import sharetrace.util.Checks;

@Buildable
public record Parameters(
    double transmissionRate,
    double sendCoefficient,
    Duration timeBuffer,
    Duration scoreExpiry,
    Duration contactExpiry,
    Duration idleTimeout) {

  public Parameters {
    Checks.checkRange(transmissionRate, Range.closed(0d, 1d), "transmissionRate");
    Checks.checkRange(sendCoefficient, Range.atLeast(0d), "sendCoefficient");
    Checks.checkRange(timeBuffer, Range.atLeast(Duration.ZERO), "timeBuffer");
    Checks.checkRange(scoreExpiry, Range.atLeast(Duration.ZERO), "scoreExpiry");
    Checks.checkRange(contactExpiry, Range.atLeast(Duration.ZERO), "contactExpiry");
    // 1 millisecond is the minimum duration supported by Akka for scheduled messages.
    Checks.checkRange(idleTimeout, Range.atLeast(Duration.ofMillis(1)), "idleTimeout");
  }
}

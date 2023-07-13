package sharetrace.model;

import com.google.common.collect.Range;
import java.time.Duration;
import org.immutables.value.Value;
import sharetrace.util.Checks;

@Value.Immutable
abstract class BaseParameters {

  public abstract float transmissionRate();

  public abstract float sendCoefficient();

  public abstract Duration timeBuffer();

  public abstract Duration scoreExpiry();

  public abstract Duration contactExpiry();

  public abstract Duration idleTimeout();

  @Value.Check
  protected void check() {
    Checks.checkRange(transmissionRate(), Range.open(0f, 1f), "transmissionRate");
    Checks.checkRange(sendCoefficient(), Range.atLeast(0f), "sendCoefficient");
    Checks.checkRange(timeBuffer(), Range.atLeast(Duration.ZERO), "timeBuffer");
    Checks.checkRange(scoreExpiry(), Range.greaterThan(Duration.ZERO), "scoreExpiry");
    Checks.checkRange(contactExpiry(), Range.greaterThan(Duration.ZERO), "contactExpiry");
    Checks.checkRange(idleTimeout(), Range.greaterThan(Duration.ZERO), "idleTimeout");
  }
}

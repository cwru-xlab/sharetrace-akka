package org.sharetrace.message;

import java.time.Duration;
import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.User;
import org.sharetrace.util.Checks;

// TODO Update
/**
 * A collection of values that modify the behavior of a {@link User} while passing messages during
 * an execution of {@link RiskPropagation}.
 */
@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseUserParams {

  // TODO Add Javadoc
  public abstract Duration idleTimeout();

  // TODO Add Javadoc
  public abstract Duration refreshPeriod();

  @Value.Check
  protected void check() {
    Checks.isGreaterThan(idleTimeout(), Duration.ZERO, "idleTimeout");
    Checks.isGreaterThan(refreshPeriod(), Duration.ZERO, "refreshPeriod");
  }
}

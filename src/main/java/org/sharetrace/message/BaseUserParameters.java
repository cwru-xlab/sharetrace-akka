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
abstract class BaseUserParameters implements UserMessage {

  @Value.Check
  protected void check() {
    Checks.greaterThan(idleTimeout(), Duration.ZERO, "idleTimeout");
    Checks.greaterThan(refreshPeriod(), Duration.ZERO, "refreshPeriod");
  }

  // TODO Add Javadoc
  public abstract Duration idleTimeout();

  // TODO Add Javadoc
  public abstract Duration refreshPeriod();
}

package org.sharetrace.message;

import java.time.Duration;
import org.immutables.value.Value;
import org.sharetrace.User;
import org.sharetrace.util.Checks;

/**
 * Parameters that modify the behavior of a {@link User}.
 *
 * @see User
 */
@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseUserParams {

  /**
   * Returns the duration after which a {@link User} terminates if it has not processed a message.
   */
  public abstract Duration idleTimeout();

  /** Returns the duration after which a {@link User} refreshes its exposure score and contacts. */
  public abstract Duration refreshPeriod();

  @Value.Check
  protected void check() {
    Checks.isGreaterThan(idleTimeout(), Duration.ZERO, "idleTimeout");
    Checks.isGreaterThan(refreshPeriod(), Duration.ZERO, "refreshPeriod");
  }
}

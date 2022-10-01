package io.sharetrace.model;

import io.sharetrace.actor.User;
import io.sharetrace.util.Checks;
import java.time.Duration;
import org.immutables.value.Value;

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

  @Value.Check
  protected void check() {
    Checks.isGreaterThan(idleTimeout(), Duration.ZERO, "idleTimeout");
  }
}

package io.sharetrace.model;

import io.sharetrace.actor.UserActor;
import io.sharetrace.util.Checks;
import java.time.Duration;
import org.immutables.value.Value;

/**
 * Parameters that modify the behavior of a {@link UserActor}.
 *
 * @see UserActor
 */
@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseUserParams {

  @Value.Check
  protected void check() {
    Checks.isGreaterThan(idleTimeout(), Duration.ZERO, "idleTimeout");
  }

  /**
   * Returns the duration after which a {@link UserActor} terminates if it has not processed a
   * message.
   */
  public abstract Duration idleTimeout();
}

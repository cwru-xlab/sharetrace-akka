package io.sharetrace.message;

import akka.actor.typed.ActorRef;
import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.actor.User;
import io.sharetrace.graph.Contact;
import io.sharetrace.util.Checks;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.immutables.value.Value;

/**
 * A message that initiates message-passing {@link User} with another {@link User}.
 *
 * @see User
 * @see RiskPropagation
 * @see Contact
 */
@Value.Immutable
abstract class BaseContactMsg implements UserMsg {

  /** Returns the time at which the two users came in contact. */
  public abstract Instant contactTime();

  /** Returns the actor reference of the contacted user. */
  public abstract ActorRef<UserMsg> contact();

  protected abstract Clock clock();

  protected abstract Duration contactTtl();

  @Value.Check
  protected void check() {
    Checks.isAtLeast(contactTime(), Instant.EPOCH, "contactTime");
  }

  public boolean isAlive() {
    return Duration.between(contactTime(), clock().instant()).compareTo(contactTtl()) < 0;
  }
}

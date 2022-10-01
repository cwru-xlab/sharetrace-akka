package io.sharetrace.message;

import akka.actor.typed.ActorRef;
import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.actor.UserActor;
import io.sharetrace.graph.Contact;
import io.sharetrace.util.Checks;
import java.time.Instant;
import org.immutables.value.Value;

/**
 * A message that initiates message-passing {@link UserActor} with another {@link UserActor}.
 *
 * @see UserActor
 * @see RiskPropagation
 * @see Contact
 */
@Value.Immutable
abstract class BaseContactMsg implements UserMsg {

  /** Returns the actor reference of the contacted user. */
  public abstract ActorRef<UserMsg> contact();

  @Value.Check
  protected void check() {
    Checks.isAtLeast(contactTime(), Instant.EPOCH, "contactTime");
  }

  /** Returns the time at which the two users came in contact. */
  public abstract Instant contactTime();
}

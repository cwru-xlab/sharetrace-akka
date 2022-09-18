package io.sharetrace.message;

import akka.actor.typed.ActorRef;
import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.actor.User;
import io.sharetrace.graph.Contact;
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
interface BaseContactMsg extends UserMsg {

  /** Returns the time at which the two users came in contact. */
  Instant contactTime();

  /** Returns the actor reference of the contacted user. */
  ActorRef<UserMsg> contact();
}

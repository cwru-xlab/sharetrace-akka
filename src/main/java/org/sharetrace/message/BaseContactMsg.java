package org.sharetrace.message;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import org.immutables.value.Value;
import org.sharetrace.RiskPropagation;
import org.sharetrace.User;
import org.sharetrace.graph.Contact;

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
  Instant time();

  /** Returns the actor reference of the contacted user. */
  ActorRef<UserMsg> replyTo();
}

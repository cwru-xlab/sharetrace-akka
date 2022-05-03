package org.sharetrace.message;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import org.immutables.value.Value;

/**
 * An interaction between two persons. As a message, a contact contains the timestamp at which the
 * two persons interacted and the actor reference of the other person. Thus, two complimentary
 * messages should be sent, one to each person of the contact, to initiate communication between
 * their actors.
 */
@Value.Immutable
interface BaseContactMessage extends NodeMessage {

  /** Returns the time at which the two individuals came in contact. */
  Instant timestamp();

  /** Returns the actor reference associated with the complimentary person of the contact. */
  ActorRef<NodeMessage> replyTo();
}

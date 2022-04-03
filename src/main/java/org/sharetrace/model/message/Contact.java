package org.sharetrace.model.message;

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
public abstract class Contact implements NodeMessage {

  public static Builder builder() {
    return ImmutableContact.builder();
  }

  /** Returns the actor reference associated with the complimentary person of the contact. */
  public abstract ActorRef<NodeMessage> replyTo();

  /** Returns the time at which the two individuals came in contact. */
  public abstract Instant timestamp();

  public interface Builder {

    Builder replyTo(ActorRef<NodeMessage> replyTo);

    Builder timestamp(Instant timestamp);

    Contact build();
  }
}

package org.sharetrace.propagation.model;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import org.immutables.value.Value;

@Value.Immutable
public abstract class Contact implements NodeMessage {

  public static Builder builder() {
    return ImmutableContact.builder();
  }

  public abstract ActorRef<NodeMessage> replyTo();

  public abstract Instant timestamp();

  public interface Builder {

    Builder replyTo(ActorRef<NodeMessage> replyTo);

    Builder timestamp(Instant timestamp);

    Contact build();
  }
}

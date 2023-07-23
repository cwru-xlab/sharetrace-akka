package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import java.time.Duration;
import java.time.Instant;
import sharetrace.model.Expirable;

public record ContactMessage(ActorRef<UserMessage> contact, Instant timestamp, Instant expiresAt)
    implements Expirable, UserMessage {

  public ContactMessage(ActorRef<UserMessage> contact, Instant timestamp, Duration expiry) {
    this(contact, timestamp, timestamp.plus(expiry));
  }
}

package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import java.time.Duration;
import java.time.Instant;
import sharetrace.model.Expirable;
import sharetrace.model.Timestamped;

public record ContactMessage(ActorRef<UserMessage> contact, Instant timestamp, Instant expiryTime)
    implements Expirable, Timestamped, UserMessage {

  public ContactMessage(ActorRef<UserMessage> contact, Instant timestamp, Duration expiry) {
    this(contact, timestamp, timestamp.plus(expiry));
  }
}

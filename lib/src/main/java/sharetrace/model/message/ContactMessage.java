package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import java.time.Duration;
import sharetrace.model.Expirable;
import sharetrace.model.Timestamp;

public record ContactMessage(
    ActorRef<UserMessage> contact, Timestamp timestamp, Timestamp expiryTime)
    implements Expirable, UserMessage {

  public ContactMessage(ActorRef<UserMessage> contact, Timestamp timestamp, Duration expiry) {
    this(contact, timestamp, timestamp.plus(expiry));
  }
}

package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import sharetrace.model.Expirable;

public record ContactMessage(ActorRef<UserMessage> contact, long timestamp, long expiryTime)
    implements Expirable, UserMessage {

  public static ContactMessage fromExpiry(
      ActorRef<UserMessage> contact, long timestamp, long expiry) {
    return new ContactMessage(contact, timestamp, Math.addExact(timestamp, expiry));
  }
}

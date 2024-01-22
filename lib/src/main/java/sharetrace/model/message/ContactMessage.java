package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import sharetrace.model.Expirable;

public record ContactMessage(ActorRef<UserMessage> contact, int id, long timestamp, long expiryTime)
    implements Expirable, UserMessage {

  public static ContactMessage fromExpiry(
      ActorRef<UserMessage> contact, int id, long timestamp, long expiry) {
    return new ContactMessage(contact, id, timestamp, Math.addExact(timestamp, expiry));
  }
}

package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import sharetrace.model.Timestamp;

public record ContactEvent(int self, int contact, Timestamp contactTime, Timestamp timestamp)
    implements UserEvent {

  public ContactEvent(
      ActorRef<?> self, ActorRef<?> contact, Timestamp contactTime, Timestamp timestamp) {
    this(UserEvent.toInt(self), UserEvent.toInt(contact), contactTime, timestamp);
  }
}

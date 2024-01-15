package sharetrace.logging.event;

import akka.actor.typed.ActorRef;

public record ContactEvent(int self, int contact, long contactTime, long timestamp)
    implements UserEvent {

  public ContactEvent(ActorRef<?> self, ActorRef<?> contact, long contactTime, long timestamp) {
    this(UserEvent.toInt(self), UserEvent.toInt(contact), contactTime, timestamp);
  }
}

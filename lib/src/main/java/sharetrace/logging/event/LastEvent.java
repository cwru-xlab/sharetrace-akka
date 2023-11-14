package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import sharetrace.model.Timestamp;

public record LastEvent(int self, Timestamp timestamp) implements UserEvent {

  public LastEvent(ActorRef<?> self, Timestamp timestamp) {
    this(UserEvent.toInt(self), timestamp);
  }
}

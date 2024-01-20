package sharetrace.logging.event.user;

import akka.actor.typed.ActorRef;

public record LastEvent(int self, long timestamp) implements UserEvent {

  public LastEvent(ActorRef<?> self, long timestamp) {
    this(UserEvent.toInt(self), timestamp);
  }
}

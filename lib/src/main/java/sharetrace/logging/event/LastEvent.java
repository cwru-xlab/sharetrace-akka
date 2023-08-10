package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;

public record LastEvent(int self, Instant timestamp) implements UserEvent {

  public LastEvent(ActorRef<?> self, Instant timestamp) {
    this(UserEvent.toInt(self), timestamp);
  }
}

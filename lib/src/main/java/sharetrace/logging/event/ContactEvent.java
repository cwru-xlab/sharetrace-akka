package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;

public record ContactEvent(int self, int contact, Instant contactTime, Instant timestamp)
    implements UserEvent {

  public ContactEvent(
      ActorRef<?> self, ActorRef<?> contact, Instant contactTime, Instant timestamp) {
    this(UserEvent.toInt(self), UserEvent.toInt(contact), contactTime, timestamp);
  }
}

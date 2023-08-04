package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;

public record ContactEvent(int self, int contact, Instant contactTime, Instant timestamp)
    implements Event {

  public ContactEvent(
      ActorRef<?> self, ActorRef<?> contact, Instant contactTime, Instant timestamp) {
    this(Event.toInt(self), Event.toInt(contact), contactTime, timestamp);
  }
}

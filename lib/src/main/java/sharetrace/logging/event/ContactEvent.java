package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;

public record ContactEvent(String self, String contact, Instant timestamp) implements Event {

  public ContactEvent(ActorRef<?> self, ActorRef<?> contact, Instant timestamp) {
    this(Event.toString(self), Event.toString(contact), timestamp);
  }
}

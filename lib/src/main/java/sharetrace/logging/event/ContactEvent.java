package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;

public record ContactEvent(String self, String contact, Instant timestamp) implements EventRecord {

  public ContactEvent(ActorRef<?> self, ActorRef<?> contact, Instant timestamp) {
    this(EventRecord.toString(self), EventRecord.toString(contact), timestamp);
  }
}

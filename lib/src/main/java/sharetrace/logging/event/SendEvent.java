package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import sharetrace.model.message.RiskScoreMessage;

public record SendEvent(String self, String contact, RiskScoreMessage message, Instant timestamp)
    implements MessageEvent {

  public SendEvent(
      ActorRef<?> self, ActorRef<?> contact, RiskScoreMessage message, Instant timestamp) {
    this(EventRecord.toString(self), EventRecord.toString(contact), message, timestamp);
  }
}

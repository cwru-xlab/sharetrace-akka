package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import sharetrace.model.message.RiskScoreMessage;

public record ReceiveEvent(String self, String contact, RiskScoreMessage message, Instant timestamp)
    implements MessageEvent {

  public ReceiveEvent(ActorRef<?> self, RiskScoreMessage message, Instant timestamp) {
    this(EventRecord.toString(self), EventRecord.toString(message.sender()), message, timestamp);
  }
}

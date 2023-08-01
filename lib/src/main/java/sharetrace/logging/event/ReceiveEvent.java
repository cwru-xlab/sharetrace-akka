package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import sharetrace.model.message.RiskScoreMessage;

public record ReceiveEvent(int self, int contact, RiskScoreMessage message, Instant timestamp)
    implements MessageEvent {

  public ReceiveEvent(ActorRef<?> self, RiskScoreMessage message, Instant timestamp) {
    this(Event.toInt(self), Event.toInt(message.sender()), message, timestamp);
  }
}

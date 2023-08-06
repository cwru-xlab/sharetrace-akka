package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import sharetrace.model.message.RiskScoreMessage;

public record ReceiveEvent(int self, int contact, RiskScoreMessage message, Instant timestamp)
    implements MessageEvent {

  public ReceiveEvent(ActorRef<?> self, RiskScoreMessage message, Instant timestamp) {
    this(UserEvent.toInt(self), UserEvent.toInt(message.sender()), message, timestamp);
  }
}

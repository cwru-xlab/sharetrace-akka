package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import sharetrace.model.message.RiskScoreMessage;

public record ReceiveEvent(int self, int contact, RiskScoreMessage message, long timestamp)
    implements MessageEvent {

  public ReceiveEvent(ActorRef<?> self, RiskScoreMessage message, long timestamp) {
    this(UserEvent.toInt(self), UserEvent.toInt(message.sender()), message, timestamp);
  }
}

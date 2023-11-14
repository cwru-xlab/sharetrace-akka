package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import sharetrace.model.Timestamp;
import sharetrace.model.message.RiskScoreMessage;

public record SendEvent(int self, int contact, RiskScoreMessage message, Timestamp timestamp)
    implements MessageEvent {

  public SendEvent(
      ActorRef<?> self, ActorRef<?> contact, RiskScoreMessage message, Timestamp timestamp) {
    this(UserEvent.toInt(self), UserEvent.toInt(contact), message, timestamp);
  }
}

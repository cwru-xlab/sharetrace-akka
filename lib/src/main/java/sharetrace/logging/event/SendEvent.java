package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import sharetrace.model.message.RiskScoreMessage;

public record SendEvent(int self, int contact, RiskScoreMessage message, Instant timestamp)
    implements MessageEvent {

  public SendEvent(
      ActorRef<?> self, ActorRef<?> contact, RiskScoreMessage message, Instant timestamp) {
    this(UserEvent.toInt(self), UserEvent.toInt(contact), message, timestamp);
  }
}

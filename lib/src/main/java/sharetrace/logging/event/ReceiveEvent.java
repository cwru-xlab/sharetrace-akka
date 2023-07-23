package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import sharetrace.model.message.RiskScoreMessage;

public record ReceiveEvent(
    ActorRef<?> self, ActorRef<?> contact, RiskScoreMessage message, Instant timestamp)
    implements MessageEvent {

  public ReceiveEvent(ActorRef<?> self, RiskScoreMessage message, Instant timestamp) {
    this(self, message.sender(), message, timestamp);
  }
}

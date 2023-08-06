package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import sharetrace.model.message.RiskScoreMessage;

public record UpdateEvent(
    int self, RiskScoreMessage previous, RiskScoreMessage current, Instant timestamp)
    implements UserEvent {

  public UpdateEvent(
      ActorRef<?> self, RiskScoreMessage previous, RiskScoreMessage current, Instant timestamp) {
    this(UserEvent.toInt(self), previous, current, timestamp);
  }
}

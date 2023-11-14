package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import sharetrace.model.Timestamp;
import sharetrace.model.message.RiskScoreMessage;

public record UpdateEvent(
    int self, RiskScoreMessage previous, RiskScoreMessage current, Timestamp timestamp)
    implements UserEvent {

  public UpdateEvent(
      ActorRef<?> self, RiskScoreMessage previous, RiskScoreMessage current, Timestamp timestamp) {
    this(UserEvent.toInt(self), previous, current, timestamp);
  }
}

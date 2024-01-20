package sharetrace.logging.event.user;

import akka.actor.typed.ActorRef;
import sharetrace.model.message.RiskScoreMessage;

public record UpdateEvent(
    int self, RiskScoreMessage previous, RiskScoreMessage current, long timestamp)
    implements UserEvent {

  public UpdateEvent(
      ActorRef<?> self, RiskScoreMessage previous, RiskScoreMessage current, long timestamp) {
    this(UserEvent.toInt(self), previous, current, timestamp);
  }
}

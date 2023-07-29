package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import sharetrace.model.message.RiskScoreMessage;

public record UpdateEvent(
    String self, RiskScoreMessage previous, RiskScoreMessage current, Instant timestamp)
    implements EventRecord {

  public UpdateEvent(
      ActorRef<?> self, RiskScoreMessage previous, RiskScoreMessage current, Instant timestamp) {
    this(EventRecord.toString(self), previous, current, timestamp);
  }
}

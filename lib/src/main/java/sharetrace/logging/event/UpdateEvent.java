package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import sharetrace.model.message.RiskScoreMessage;

public record UpdateEvent(
    ActorRef<?> self, RiskScoreMessage previous, RiskScoreMessage current, Instant timestamp)
    implements EventRecord {}

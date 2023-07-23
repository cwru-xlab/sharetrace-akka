package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import sharetrace.model.message.RiskScoreMessage;

public record SendEvent(
    ActorRef<?> self, ActorRef<?> contact, RiskScoreMessage message, Instant timestamp)
    implements MessageEvent {}

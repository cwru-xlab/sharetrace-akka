package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import sharetrace.logging.ActorRefSerializer;

public record ContactEvent(
    ActorRef<?> self,
    @JsonSerialize(using = ActorRefSerializer.class) ActorRef<?> contact,
    Instant timestamp)
    implements EventRecord {}

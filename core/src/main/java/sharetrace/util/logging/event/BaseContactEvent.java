package sharetrace.util.logging.event;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;
import sharetrace.util.logging.ActorRefSerializer;

@Value.Immutable
interface BaseContactEvent extends EventRecord {

  @JsonSerialize(using = ActorRefSerializer.class)
  ActorRef<?> contact();
}

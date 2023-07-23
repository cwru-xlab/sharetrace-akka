package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import sharetrace.logging.ActorRefSerializer;
import sharetrace.logging.LogRecord;
import sharetrace.model.Timestamped;

public interface EventRecord extends LogRecord, Timestamped {

  @JsonSerialize(using = ActorRefSerializer.class)
  ActorRef<?> self();
}

package sharetrace.util.logging.event;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import sharetrace.model.Timestamped;
import sharetrace.util.logging.ActorRefSerializer;
import sharetrace.util.logging.LogRecord;

public interface EventRecord extends LogRecord, Timestamped {

  String KEY = "event";

  @JsonSerialize(using = ActorRefSerializer.class)
  ActorRef<?> self();
}

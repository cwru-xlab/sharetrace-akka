package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import sharetrace.logging.ActorRefSerializer;
import sharetrace.model.message.RiskScoreMessage;

public interface MessageEvent extends EventRecord {

  RiskScoreMessage message();

  @JsonSerialize(using = ActorRefSerializer.class)
  ActorRef<?> contact();
}

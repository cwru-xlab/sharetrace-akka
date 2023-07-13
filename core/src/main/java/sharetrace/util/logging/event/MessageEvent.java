package sharetrace.util.logging.event;

import akka.actor.typed.ActorRef;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.util.logging.ActorRefSerializer;

interface MessageEvent extends EventRecord {

  RiskScoreMessage message();

  @JsonSerialize(using = ActorRefSerializer.class)
  ActorRef<?> contact();
}

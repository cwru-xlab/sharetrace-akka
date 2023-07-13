package sharetrace.model.message;

import akka.actor.typed.ActorRef;
import org.immutables.value.Value;
import sharetrace.model.Expirable;

@Value.Immutable
interface BaseContactMessage extends Expirable, UserMessage {

  ActorRef<UserMessage> contact();
}

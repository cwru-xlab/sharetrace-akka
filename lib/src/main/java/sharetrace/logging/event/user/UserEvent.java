package sharetrace.logging.event.user;

import akka.actor.typed.ActorRef;
import sharetrace.logging.event.Event;

public interface UserEvent extends Event {

  static int toInt(ActorRef<?> ref) {
    return Integer.parseInt(ref.path().name());
  }

  int self();
}

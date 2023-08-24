package sharetrace.logging.event;

import akka.actor.typed.ActorRef;

public interface UserEvent extends Event {

  static int toInt(ActorRef<?> ref) {
    return Integer.parseInt(ref.path().name());
  }

  int self();
}

package sharetrace.logging.event;

import akka.actor.typed.ActorRef;

public interface UserEvent extends Event {

  static int toInt(ActorRef<?> actorRef) {
    return Integer.parseInt(actorRef.path().name());
  }

  int self();
}

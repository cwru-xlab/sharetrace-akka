package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import sharetrace.logging.LogRecord;
import sharetrace.model.Timestamped;

public interface Event extends LogRecord, Timestamped {

  String self();

  static String toString(ActorRef<?> actorRef) {
    return actorRef.path().name();
  }
}

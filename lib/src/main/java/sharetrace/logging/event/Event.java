package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import sharetrace.logging.LogRecord;
import sharetrace.model.Timestamped;

public interface Event extends LogRecord, Timestamped {

  int self();

  static int toInt(ActorRef<?> actorRef) {
    return Integer.parseInt(actorRef.path().name());
  }
}

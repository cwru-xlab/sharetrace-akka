package sharetrace.logging.event;

import akka.actor.typed.ActorRef;
import sharetrace.logging.LogRecord;
import sharetrace.model.Timestamped;

public interface Event extends LogRecord, Timestamped {

  static String key() {
    return "event";
  }

  int self();

  static int toInt(ActorRef<?> actorRef) {
    return Integer.parseInt(actorRef.path().name());
  }
}

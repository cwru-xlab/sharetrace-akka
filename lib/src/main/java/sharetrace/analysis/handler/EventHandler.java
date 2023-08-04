package sharetrace.analysis.handler;

import sharetrace.logging.event.Event;

public interface EventHandler {

  default void onNext(Event event) {}

  default void onComplete() {}
}

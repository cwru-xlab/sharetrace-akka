package sharetrace.analysis.handler;

import sharetrace.analysis.results.Results;
import sharetrace.logging.event.Event;

public interface EventHandler {

  void onNext(Event event);

  void onComplete(Results results);
}

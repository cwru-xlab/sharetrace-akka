package sharetrace.analysis.handler;

import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.Results;
import sharetrace.logging.event.Event;

public interface EventHandler {

  void onNext(Event event, Context context);

  void onComplete(Results results, Context context);
}

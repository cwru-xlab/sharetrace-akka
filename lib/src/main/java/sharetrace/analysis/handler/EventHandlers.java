package sharetrace.analysis.handler;

import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.Results;
import sharetrace.logging.event.Event;

public record EventHandlers(Iterable<? extends EventHandler> handlers) implements EventHandler {

  @Override
  public void onNext(Event event, Context context) {
    handlers.forEach(handler -> handler.onNext(event, context));
  }

  @Override
  public void onComplete(Results results, Context context) {
    handlers.forEach(handler -> handler.onComplete(results, context));
  }
}

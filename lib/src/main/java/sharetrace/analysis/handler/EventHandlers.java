package sharetrace.analysis.handler;

import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.EventRecord;
import sharetrace.analysis.model.Results;

public record EventHandlers(Iterable<? extends EventHandler> handlers) implements EventHandler {

  @Override
  public void onNext(EventRecord record, Context context) {
    handlers.forEach(handler -> handler.onNext(record, context));
  }

  @Override
  public void onComplete(Results results, Context context) {
    handlers.forEach(handler -> handler.onComplete(results, context));
  }
}

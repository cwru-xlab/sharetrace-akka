package sharetrace.analysis.handler;

import sharetrace.analysis.results.Results;
import sharetrace.logging.event.Event;

public record EventHandlers(Iterable<? extends EventHandler> handlers) implements EventHandler {

  @Override
  public void onNext(Event event) {
    handlers.forEach(handler -> handler.onNext(event));
  }

  @Override
  public void onComplete(Results results) {
    handlers.forEach(handler -> handler.onComplete(results));
  }
}

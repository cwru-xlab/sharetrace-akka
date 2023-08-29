package sharetrace.analysis.handler;

import java.util.Collection;
import sharetrace.analysis.collector.ResultsCollector;
import sharetrace.logging.event.Event;

public final class EventHandlers implements EventHandler {

  private final Collection<? extends EventHandler> handlers;

  public EventHandlers(Collection<? extends EventHandler> handlers) {
    this.handlers = handlers;
  }

  @Override
  public void onNext(Event event) {
    handlers.forEach(handler -> handler.onNext(event));
  }

  @Override
  public void onComplete(ResultsCollector collector) {
    handlers.forEach(handler -> handler.onComplete(collector));
  }
}

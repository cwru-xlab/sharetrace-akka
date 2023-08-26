package sharetrace.analysis.handler;

import java.util.Collection;
import java.util.List;
import sharetrace.analysis.appender.ResultsCollector;
import sharetrace.logging.event.Event;

public final class EventHandlers implements EventHandler {

  private final String key;
  private final Collection<? extends EventHandler> handlers;

  public EventHandlers(String key, Collection<? extends EventHandler> handlers) {
    this.key = key;
    this.handlers = handlers;
  }

  public EventHandlers(String key, EventHandler... handlers) {
    this(key, List.of(handlers));
  }

  @Override
  public void onNext(Event event) {
    handlers.forEach(handler -> handler.onNext(event));
  }

  @Override
  public void onComplete(ResultsCollector collector) {
    handlers.forEach(handler -> handler.onComplete(collector.withHandlerKey(key)));
  }
}

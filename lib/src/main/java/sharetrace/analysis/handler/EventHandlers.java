package sharetrace.analysis.handler;

import java.util.List;
import sharetrace.logging.event.Event;

public final class EventHandlers implements EventHandler {

  private final String key;
  private final Iterable<? extends EventHandler> handlers;

  public EventHandlers(String key, Iterable<? extends EventHandler> handlers) {
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
  public void onComplete() {
    handlers.forEach(EventHandler::onComplete);
  }
}

package sharetrace.analysis.handler;

import java.util.List;
import sharetrace.logging.event.Event;

public final class EventHandlers implements EventHandler {

  private final Iterable<? extends EventHandler> handlers;

  public EventHandlers(Iterable<? extends EventHandler> handlers) {
    this.handlers = handlers;
  }

  public EventHandlers(EventHandler... handlers) {
    this(List.of(handlers));
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

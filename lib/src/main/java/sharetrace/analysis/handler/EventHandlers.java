package sharetrace.analysis.handler;

import java.util.Collection;
import sharetrace.analysis.collector.ResultsCollector;
import sharetrace.logging.event.Event;

public record EventHandlers(Collection<? extends EventHandler> handlers) implements EventHandler {

  @Override
  public void onNext(Event event) {
    handlers.forEach(handler -> handler.onNext(event));
  }

  @Override
  public void onComplete(ResultsCollector collector) {
    handlers.forEach(handler -> handler.onComplete(collector));
  }
}

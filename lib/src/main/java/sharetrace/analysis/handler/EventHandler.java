package sharetrace.analysis.handler;

import sharetrace.analysis.collector.ResultsCollector;
import sharetrace.logging.event.Event;

public interface EventHandler {

  void onNext(Event event);

  void onComplete(ResultsCollector collector);
}

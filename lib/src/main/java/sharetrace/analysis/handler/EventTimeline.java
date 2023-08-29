package sharetrace.analysis.handler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.collector.ResultsCollector;
import sharetrace.analysis.model.LoggedEvent;
import sharetrace.logging.event.Event;
import sharetrace.util.Instants;

public final class EventTimeline implements EventHandler {

  private final List<LoggedEvent> timeline;
  private Instant min;

  public EventTimeline() {
    timeline = new ArrayList<>();
    min = Instant.MAX;
  }

  @Override
  public void onNext(Event event) {
    var logged = LoggedEvent.from(event);
    min = Instants.min(min, logged.timestamp());
    timeline.add(logged);
  }

  @Override
  public void onComplete(ResultsCollector collector) {
    timeline.replaceAll(this::adjustTimestamp);
    timeline.sort(Comparator.comparing(LoggedEvent::timestamp));
    collector.put("timeline", timeline);
  }

  private LoggedEvent adjustTimestamp(LoggedEvent event) {
    var offset = Duration.between(min, event.timestamp());
    return new LoggedEvent(event.type(), Instant.EPOCH.plus(offset));
  }
}

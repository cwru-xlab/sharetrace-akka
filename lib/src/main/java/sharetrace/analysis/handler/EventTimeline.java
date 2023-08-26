package sharetrace.analysis.handler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.appender.ResultsCollector;
import sharetrace.analysis.model.LoggedEvent;
import sharetrace.logging.event.Event;
import sharetrace.util.Instants;

public final class EventTimeline implements EventHandler {

  private final List<LoggedEvent> timeline;

  private Instant min;

  public EventTimeline() {
    timeline = new ArrayList<>();
    min = Instant.EPOCH;
  }

  @Override
  public void onNext(Event event) {
    var loggedEvent = LoggedEvent.from(event);
    min = Instants.min(min, loggedEvent.timestamp());
    timeline.add(loggedEvent);
  }

  @Override
  public void onComplete(ResultsCollector collector) {
    timeline.replaceAll(this::adjustTimestamp);
    timeline.sort(Comparator.comparing(LoggedEvent::timestamp));
  }

  private LoggedEvent adjustTimestamp(LoggedEvent event) {
    var timestamp = event.timestamp();
    var offset = Duration.between(min, timestamp);
    return new LoggedEvent(event.type(), timestamp.minus(offset));
  }
}

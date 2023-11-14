package sharetrace.analysis.handler;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.model.LoggedEvent;
import sharetrace.analysis.results.Results;
import sharetrace.logging.event.Event;
import sharetrace.model.Timestamp;

public final class EventTimeline implements EventHandler {

  private final List<LoggedEvent> timeline;
  private Timestamp min;

  public EventTimeline() {
    timeline = new ArrayList<>();
    min = Timestamp.MAX;
  }

  @Override
  public void onNext(Event event) {
    var logged = LoggedEvent.from(event);
    min = Timestamp.min(min, logged.timestamp());
    timeline.add(logged);
  }

  @Override
  public void onComplete(Results results) {
    timeline.replaceAll(this::adjustTimestamp);
    timeline.sort(Comparator.comparing(LoggedEvent::timestamp));
    results.put("timeline", timeline);
  }

  private LoggedEvent adjustTimestamp(LoggedEvent event) {
    var offset = Duration.between(min, event.timestamp());
    return new LoggedEvent(event.type(), Timestamp.MIN.plus(offset));
  }
}

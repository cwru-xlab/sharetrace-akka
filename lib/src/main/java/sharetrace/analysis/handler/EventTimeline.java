package sharetrace.analysis.handler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.model.LoggedEvent;
import sharetrace.analysis.results.Results;
import sharetrace.logging.event.Event;

public final class EventTimeline implements EventHandler {

  private final List<LoggedEvent> timeline;

  private long min;

  public EventTimeline() {
    timeline = new ArrayList<>();
    min = Long.MAX_VALUE;
  }

  @Override
  public void onNext(Event event) {
    var logged = LoggedEvent.from(event);
    min = Math.min(min, logged.timestamp());
    timeline.add(logged);
  }

  @Override
  public void onComplete(Results results) {
    timeline.replaceAll(this::adjustTimestamp);
    timeline.sort(Comparator.comparing(LoggedEvent::timestamp));
    results.put("timeline", timeline);
  }

  private LoggedEvent adjustTimestamp(LoggedEvent event) {
    var offset = Math.subtractExact(event.timestamp(), min);
    return new LoggedEvent(event.type(), offset);
  }
}

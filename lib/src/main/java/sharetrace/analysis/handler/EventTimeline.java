package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.model.LoggedEvent;
import sharetrace.analysis.results.Results;
import sharetrace.logging.event.Event;

public final class EventTimeline implements EventHandler {

  private final List<LoggedEvent> timeline;

  private long minTimestamp;

  public EventTimeline() {
    timeline = new ReferenceArrayList<>();
    minTimestamp = Long.MAX_VALUE;
  }

  @Override
  public void onNext(Event event) {
    var logged = LoggedEvent.from(event);
    minTimestamp = Math.min(minTimestamp, logged.timestamp());
    timeline.add(logged);
  }

  @Override
  public void onComplete(Results results) {
    timeline.replaceAll(this::adjustTimestamp);
    timeline.sort(Comparator.comparing(LoggedEvent::timestamp));
    results.put("timeline", timeline);
  }

  private LoggedEvent adjustTimestamp(LoggedEvent event) {
    return new LoggedEvent(event.type(), event.timestamp() - minTimestamp);
  }
}

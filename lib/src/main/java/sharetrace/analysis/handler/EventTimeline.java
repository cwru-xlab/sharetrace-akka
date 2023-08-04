package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.model.LoggedEvent;
import sharetrace.logging.event.Event;

public final class EventTimeline implements EventHandler {

  private final List<LoggedEvent> timeline;

  private Instant min;

  public EventTimeline() {
    timeline = new ObjectArrayList<>();
    min = Instant.MIN;
  }

  @Override
  public void onNext(Event event) {
    var loggedEvent = LoggedEvent.from(event);
    var timestamp = loggedEvent.timestamp();
    min = timestamp.isBefore(min) ? timestamp : min;
    timeline.add(loggedEvent);
  }

  @Override
  public void onComplete() {
    timeline.replaceAll(this::adjustTimestamps);
    timeline.sort(Comparator.comparing(LoggedEvent::timestamp));
  }

  private LoggedEvent adjustTimestamps(LoggedEvent event) {
    var timestamp = event.timestamp();
    var offset = Duration.between(min, timestamp);
    return new LoggedEvent(event.type(), timestamp.minus(offset));
  }
}

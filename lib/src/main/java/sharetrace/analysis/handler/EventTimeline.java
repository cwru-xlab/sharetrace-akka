package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.model.LoggedEvent;
import sharetrace.logging.event.Event;
import sharetrace.util.Instants;

public final class EventTimeline implements EventHandler {

  private final List<LoggedEvent> timeline;

  private Instant min;

  public EventTimeline() {
    timeline = new ObjectArrayList<>();
    min = Instant.EPOCH;
  }

  @Override
  public void onNext(Event event) {
    var loggedEvent = LoggedEvent.from(event);
    var timestamp = loggedEvent.timestamp();
    min = Instants.min(min, timestamp);
    timeline.add(loggedEvent);
  }

  @Override
  public void onComplete() {
    timeline.replaceAll(this::adjustTimestamp);
    timeline.sort(Comparator.comparing(LoggedEvent::timestamp));
  }

  private LoggedEvent adjustTimestamp(LoggedEvent event) {
    var timestamp = event.timestamp();
    var offset = Duration.between(min, timestamp);
    return new LoggedEvent(event.type(), timestamp.minus(offset));
  }
}

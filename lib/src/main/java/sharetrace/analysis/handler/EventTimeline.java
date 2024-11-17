package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.EventRecord;
import sharetrace.analysis.model.Results;
import sharetrace.logging.event.Event;

public final class EventTimeline implements EventHandler {

  private final List<TimelineEvent> timeline;

  private long minTimestamp;

  public EventTimeline() {
    timeline = new ReferenceArrayList<>();
    minTimestamp = Long.MAX_VALUE;
  }

  @Override
  public void onNext(EventRecord record, Context context) {
    minTimestamp = Math.min(minTimestamp, record.timestamp());
    timeline.add(new TimelineEvent(record.event().getClass(), record.timestamp()));
  }

  @Override
  public void onComplete(Results results, Context context) {
    timeline.replaceAll(this::adjustTimestamp);
    timeline.sort(Comparator.comparingLong(TimelineEvent::timestamp));
    results.put("timeline", timeline);
  }

  private TimelineEvent adjustTimestamp(TimelineEvent event) {
    return new TimelineEvent(event.type(), event.timestamp() - minTimestamp);
  }

  private record TimelineEvent(Class<? extends Event> type, long timestamp) implements Event {}
}

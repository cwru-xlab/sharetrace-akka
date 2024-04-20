package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.Results;
import sharetrace.analysis.model.TypedEvent;
import sharetrace.logging.event.Event;

public final class EventTimeline implements EventHandler {

  private final List<TypedEvent> timeline;

  private long minTimestamp;

  public EventTimeline() {
    timeline = new ReferenceArrayList<>();
    minTimestamp = Long.MAX_VALUE;
  }

  @Override
  public void onNext(Event event, Context context) {
    minTimestamp = Math.min(minTimestamp, event.timestamp());
    timeline.add(new TypedEvent(event.getClass(), event.timestamp()));
  }

  @Override
  public void onComplete(Results results, Context context) {
    timeline.replaceAll(this::adjustTimestamp);
    timeline.sort(Comparator.comparing(TypedEvent::timestamp));
    results.put("timeline", timeline);
  }

  private TypedEvent adjustTimestamp(TypedEvent event) {
    return new TypedEvent(event.type(), event.timestamp() - minTimestamp);
  }
}

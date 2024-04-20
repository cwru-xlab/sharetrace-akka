package sharetrace.analysis.handler;

import java.util.Map;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.Results;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.user.UserEvent;

public final class UserEventCounts implements EventHandler {

  private final Map<Class<?>, int[]> counts;

  public UserEventCounts() {
    counts = new Object2ReferenceOpenHashMap<>();
  }

  @Override
  public void onNext(Event event, Context context) {
    if (event instanceof UserEvent e) {
      var eventCounts = counts.computeIfAbsent(e.getClass(), x -> new int[context.nodes()]);
      eventCounts[e.self()]++;
    }
  }

  @Override
  public void onComplete(Results results, Context context) {
    results.withScope("user").put("counts", counts);
  }
}

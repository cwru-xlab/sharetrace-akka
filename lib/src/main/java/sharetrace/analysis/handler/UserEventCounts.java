package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.Map;
import sharetrace.analysis.results.Results;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.user.UserEvent;

public final class UserEventCounts implements EventHandler {

  private final Map<Integer, Reference2IntMap<Class<? extends UserEvent>>> counts;

  public UserEventCounts() {
    counts = new Int2ReferenceOpenHashMap<>();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof UserEvent e) {
      counts
          .computeIfAbsent(e.self(), x -> new Reference2IntOpenHashMap<>())
          .mergeInt(e.getClass(), 1, Integer::sum);
    }
  }

  @Override
  public void onComplete(Results results) {
    results.withScope("user").put("events", counts);
  }
}

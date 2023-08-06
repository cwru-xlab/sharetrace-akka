package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Map;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.UserEvent;

public final class UserEventCounter implements EventHandler {

  private final Map<Integer, Object2IntMap<Class<?>>> counts;

  public UserEventCounter() {
    counts = new Int2ObjectOpenHashMap<>();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof UserEvent userEvent) {
      counts
          .computeIfAbsent(userEvent.self(), x -> new Object2IntOpenHashMap<>())
          .mergeInt(userEvent.getClass(), 1, Integer::sum);
    }
  }
}

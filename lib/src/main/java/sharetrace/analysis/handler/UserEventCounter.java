package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import sharetrace.analysis.results.Results;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.UserEvent;

public final class UserEventCounter implements EventHandler {

  private final Int2ObjectMap<Object2IntMap<String>> counts;

  public UserEventCounter() {
    counts = new Int2ObjectOpenHashMap<>();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof UserEvent userEvent) {
      counts
          .computeIfAbsent(userEvent.self(), x -> new Object2IntOpenHashMap<>())
          .mergeInt(userEvent.getClass().getSimpleName(), 1, Integer::sum);
    }
  }

  @Override
  public void onComplete(Results results) {
    results.withScope("user").put("events", counts);
  }
}

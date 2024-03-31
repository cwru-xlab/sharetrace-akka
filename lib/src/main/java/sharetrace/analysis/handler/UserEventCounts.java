package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import sharetrace.analysis.results.Results;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.user.UserEvent;

public final class UserEventCounts implements EventHandler {

  private final Reference2IntMap<Class<? extends UserEvent>> encoding;
  private final Int2ReferenceMap<IntList> counts;

  public UserEventCounts() {
    encoding = new Reference2IntOpenHashMap<>();
    counts = new Int2ReferenceOpenHashMap<>();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof UserEvent e) {
      var index = updateAndGetIndex(e);
      var userCounts = updateAndGetCounts(e);
      updateCounts(userCounts, index);
    }
  }

  private int updateAndGetIndex(UserEvent event) {
    return encoding.computeIfAbsent(event.getClass(), x -> encoding.size());
  }

  private IntList updateAndGetCounts(UserEvent event) {
    return counts.computeIfAbsent(event.self(), x -> new IntArrayList());
  }

  private void updateCounts(IntList counts, int event) {
    if (counts.size() <= event) {
      counts.size(event + 1);
    }
    counts.set(event, counts.getInt(event) + 1);
  }

  @Override
  public void onComplete(Results results) {
    results.withScope("user").withScope("events").put("counts", counts).put("encoding", encoding);
  }
}

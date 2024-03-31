package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.Map;
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
      var index = encoding.computeIfAbsent(e.getClass(), x -> encoding.size());
      var userCounts = counts.computeIfAbsent(e.self(), x -> new IntArrayList());
      userCounts.size(index + 1);
      userCounts.set(index, userCounts.getInt(index) + 1);
    }
  }

  @Override
  public void onComplete(Results results) {
    results.withScope("user").withScope("events").put("counts", counts).put("encoding", encoding);
  }
}

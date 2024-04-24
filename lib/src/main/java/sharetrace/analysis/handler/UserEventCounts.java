package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import java.util.Map;
import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.EventRecord;
import sharetrace.analysis.model.Results;
import sharetrace.logging.event.user.UserEvent;

public final class UserEventCounts implements EventHandler {

  private final Map<Class<?>, int[]> counts;

  public UserEventCounts() {
    counts = new Object2ReferenceOpenHashMap<>();
  }

  @Override
  public void onNext(EventRecord record, Context context) {
    if (record.event() instanceof UserEvent e) {
      counts.computeIfAbsent(e.getClass(), x -> new int[context.nodes()])[e.self()]++;
    }
  }

  @Override
  public void onComplete(Results results, Context context) {
    results.withScope("user").put("counts", counts);
  }
}

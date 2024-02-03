package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import sharetrace.analysis.results.Results;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.user.UpdateEvent;
import sharetrace.model.RiskScore;
import sharetrace.model.message.RiskScoreMessage;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class UserUpdates implements EventHandler {

  private final Map<Integer, List> updates;

  public UserUpdates() {
    updates = new Int2ReferenceOpenHashMap<>();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof UpdateEvent update) {
      updates.computeIfAbsent(update.self(), x -> new ReferenceArrayList<>()).add(update);
    }
  }

  @Override
  public void onComplete(Results results) {
    updates.replaceAll((user, updates) -> scores(updates));
    updates.values().removeIf(List::isEmpty);
    results.withScope("user").put("updates", updates);
  }

  private List<RiskScore> scores(Collection<UpdateEvent> updates) {
    return updates.stream()
        .sorted(Comparator.comparing(UpdateEvent::timestamp))
        .skip(1) // First update is trivial: default risk score -> initial non-zero risk score
        .map(UpdateEvent::current)
        .map(RiskScoreMessage::score)
        .collect(ReferenceArrayList.toList());
  }
}

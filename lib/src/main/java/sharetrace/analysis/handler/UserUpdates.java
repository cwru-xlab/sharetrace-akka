package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.results.Results;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.UpdateEvent;
import sharetrace.model.RiskScore;
import sharetrace.model.message.RiskScoreMessage;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class UserUpdates implements EventHandler {

  private final Int2ObjectMap<List> updates;

  public UserUpdates() {
    updates = new Int2ObjectOpenHashMap<>();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof UpdateEvent update) {
      updates.computeIfAbsent(update.self(), x -> new ArrayList<>()).add(update);
    }
  }

  @Override
  public void onComplete(Results results) {
    updates.replaceAll((user, updates) -> scores(updates));
    updates.values().removeIf(List::isEmpty);
    results.withScope("user").put("updates", updates);
  }

  private List<RiskScore> scores(List<UpdateEvent> updates) {
    return updates.stream()
        .sorted(Comparator.comparing(UpdateEvent::timestamp))
        .skip(1) // First update is trivial: default risk score -> initial non-zero risk score
        .map(UpdateEvent::current)
        .map(RiskScoreMessage::score)
        .toList();
  }
}

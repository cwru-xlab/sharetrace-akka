package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.collector.ResultsCollector;
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
  public void onComplete(ResultsCollector collector) {
    updates.replaceAll((user, updates) -> scores(updates));
    updates.values().removeIf(List::isEmpty);
    collector.withScope("user").put("updates", Int2ObjectMaps.unmodifiable(updates));
  }

  private List<RiskScore> scores(List<UpdateEvent> updates) {
    return updates.stream()
        .skip(1)
        .sorted(Comparator.comparing(UpdateEvent::timestamp))
        .map(UpdateEvent::current)
        .map(RiskScoreMessage::score)
        .toList();
  }
}

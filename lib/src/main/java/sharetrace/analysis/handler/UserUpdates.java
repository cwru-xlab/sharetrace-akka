package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.UpdateEvent;
import sharetrace.model.RiskScore;
import sharetrace.model.message.RiskScoreMessage;

public final class UserUpdates implements EventHandler {

  private final Map<Integer, List<UpdateEvent>> updates;

  public UserUpdates() {
    updates = new Int2ObjectOpenHashMap<>();
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof UpdateEvent update) {
      updates.computeIfAbsent(update.self(), x -> new ObjectArrayList<>()).add(update);
    }
  }

  @Override
  public void onComplete() {
    var selected = new Int2ObjectOpenHashMap<>(updates.size());
    updates.forEach((user, updates) -> selected.put((int) user, selected(updates)));
  }

  private List<RiskScore> selected(List<UpdateEvent> updates) {
    updates.sort(Comparator.comparing(UpdateEvent::timestamp));
    var selected = new ObjectArrayList<RiskScore>(updates.size() + 1);
    var first = updates.get(0);
    selected.add(first.previous().score());
    selected.add(first.current().score());
    IntStream.range(1, updates.size())
        .mapToObj(updates::get)
        .map(UpdateEvent::current)
        .map(RiskScoreMessage::score)
        .forEach(selected::add);
    return selected;
  }
}

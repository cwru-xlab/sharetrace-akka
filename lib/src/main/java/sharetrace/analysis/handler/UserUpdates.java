package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.Results;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.user.UpdateEvent;
import sharetrace.model.RiskScore;
import sharetrace.model.message.RiskScoreMessage;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class UserUpdates implements EventHandler {

  private List[] updates;

  public UserUpdates() {}

  @Override
  public void onNext(Event event, Context context) {
    if (event instanceof UpdateEvent e) {
      if (updates == null) {
        updates = new List[context.nodes()];
      }
      var index = e.self();
      if (updates[index] == null) {
        updates[index] = new ReferenceArrayList<>();
      }
      updates[index].add(e);
    }
  }

  @Override
  public void onComplete(Results results, Context context) {
    for (var i = 0; i < updates.length; i++) {
      updates[i] = scores(updates[i]);
    }
    results.withScope("user").put("updates", updates);
  }

  private List<RiskScore> scores(Collection<UpdateEvent> updates) {
    return updates.stream()
        .sorted(Comparator.comparing(UpdateEvent::timestamp))
        .skip(1) // First update is trivial: default risk score -> initial risk score
        .map(UpdateEvent::current)
        .map(RiskScoreMessage::score)
        .collect(ReferenceArrayList.toList());
  }
}

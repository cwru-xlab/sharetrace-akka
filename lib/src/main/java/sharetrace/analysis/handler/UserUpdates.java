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

  private final ReferenceArrayList<List> updates;

  private boolean initialized;

  public UserUpdates() {
    updates = new ReferenceArrayList<>();
  }

  @Override
  public void onNext(Event event, Context context) {
    if (event instanceof UpdateEvent e) {
      getUpdates(context).get(e.self()).add(e);
    }
  }

  private ReferenceArrayList<List> getUpdates(Context context) {
    if (!initialized) {
      updates.ensureCapacity(context.nodes());
      updates.replaceAll(x -> new ReferenceArrayList<>());
      initialized = true;
    }
    return updates;
  }

  @Override
  public void onComplete(Results results, Context context) {
    updates.replaceAll(this::scores);
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

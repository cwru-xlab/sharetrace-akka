package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.Results;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.user.UpdateEvent;

public final class UserUpdates implements EventHandler {

  private List<List<UpdateEvent>> updates;

  public UserUpdates() {}

  @Override
  public void onNext(Event event, Context context) {
    if (event instanceof UpdateEvent e) {
      getUpdates(context).get(e.self()).add(e);
    }
  }

  private List<List<UpdateEvent>> getUpdates(Context context) {
    if (updates == null) {
      var list = new ReferenceArrayList<List<UpdateEvent>>();
      list.ensureCapacity(context.nodes());
      list.replaceAll(x -> new ReferenceArrayList<>());
      updates = list;
    }
    return updates;
  }

  @Override
  public void onComplete(Results results, Context context) {
    results.withScope("user").put("updates", totalUpdates());
  }

  private double[] totalUpdates() {
    var totals = new double[updates.size()];
    for (var i = 0; i < updates.size(); i++) {
      var iUpdates = updates.get(i);
      if (iUpdates.size() > 1) {
        iUpdates.sort(Comparator.comparing(UpdateEvent::timestamp));
        var last = iUpdates.getLast().current().value();
        var first = iUpdates.getFirst().current().value();
        totals[i] = last - first;
      }
    }
    return totals;
  }
}

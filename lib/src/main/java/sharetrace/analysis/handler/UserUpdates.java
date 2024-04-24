package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import java.util.Comparator;
import java.util.List;
import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.EventRecord;
import sharetrace.analysis.model.Results;
import sharetrace.logging.event.user.UpdateEvent;

public final class UserUpdates implements EventHandler {

  private List<List<EventRecord>> updates;

  public UserUpdates() {}

  @Override
  public void onNext(EventRecord record, Context context) {
    if (record.event() instanceof UpdateEvent e) {
      getUpdates(context).get(e.self()).add(record);
    }
  }

  private List<List<EventRecord>> getUpdates(Context context) {
    if (updates == null) {
      var list = new ReferenceArrayList<List<EventRecord>>();
      list.size(context.nodes());
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
        iUpdates.sort(Comparator.comparing(EventRecord::timestamp));
        totals[i] = getValue(iUpdates.getLast()) - getValue(iUpdates.getFirst());
      }
    }
    return totals;
  }

  private double getValue(EventRecord record) {
    return ((UpdateEvent) record.event()).current().value();
  }
}

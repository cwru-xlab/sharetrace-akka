package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import java.util.stream.Stream;
import sharetrace.analysis.model.Context;
import sharetrace.analysis.model.EventRecord;
import sharetrace.analysis.model.Results;
import sharetrace.logging.event.lifecycle.CreateUsersEnd;
import sharetrace.logging.event.lifecycle.CreateUsersStart;
import sharetrace.logging.event.lifecycle.LifecycleEvent;
import sharetrace.logging.event.lifecycle.RiskPropagationEnd;
import sharetrace.logging.event.lifecycle.RiskPropagationStart;
import sharetrace.logging.event.lifecycle.SendContactsEnd;
import sharetrace.logging.event.lifecycle.SendContactsStart;
import sharetrace.logging.event.lifecycle.SendRiskScoresEnd;
import sharetrace.logging.event.lifecycle.SendRiskScoresStart;
import sharetrace.logging.event.user.LastEvent;

public final class Runtimes implements EventHandler {

  private static final long UNKNOWN_RUNTIME = -1;

  private final Reference2LongMap<Class<?>> events;

  private long lastEventTime;

  public Runtimes() {
    events = new Reference2LongOpenHashMap<>();
    lastEventTime = Long.MIN_VALUE;
  }

  @Override
  public void onNext(EventRecord record, Context context) {
    if (record.event() instanceof LifecycleEvent e) {
      events.put(e.getClass(), record.timestamp());
    } else if (record.event() instanceof LastEvent e) {
      lastEventTime = Math.max(lastEventTime, e.timestamp());
    }
  }

  @Override
  public void onComplete(Results results, Context context) {
    results
        .withScope("runtime")
        .put("CreateUsers", getRuntime(CreateUsersStart.class, CreateUsersEnd.class))
        .put("SendContacts", getRuntime(SendContactsStart.class, SendContactsEnd.class))
        .put("SendScores", getRuntime(SendRiskScoresStart.class, SendRiskScoresEnd.class))
        .put("RiskPropagation", getRuntime(RiskPropagationStart.class, RiskPropagationEnd.class))
        .put("MessagePassing", messagePassingRuntime());
  }

  private Object getRuntime(Class<?> start, Class<?> end) {
    return isLogged(start, end) ? events.getLong(end) - events.getLong(start) : UNKNOWN_RUNTIME;
  }

  private Object messagePassingRuntime() {
    var start = SendRiskScoresStart.class;
    return isLogged(start) ? lastEventTime - events.getLong(start) : UNKNOWN_RUNTIME;
  }

  private boolean isLogged(Class<?>... types) {
    return Stream.of(types).allMatch(events::containsKey);
  }
}

package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.Reference2LongMap;
import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;
import java.util.stream.Stream;
import sharetrace.analysis.results.Results;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.lifecycle.CreateUsersEnd;
import sharetrace.logging.event.lifecycle.CreateUsersStart;
import sharetrace.logging.event.lifecycle.LifecycleEvent;
import sharetrace.logging.event.lifecycle.RiskPropagationEnd;
import sharetrace.logging.event.lifecycle.RiskPropagationStart;
import sharetrace.logging.event.lifecycle.SendContactsEnd;
import sharetrace.logging.event.lifecycle.SendContactsStart;
import sharetrace.logging.event.lifecycle.SendRiskScoresEnd;
import sharetrace.logging.event.lifecycle.SendRiskScoresStart;
import sharetrace.logging.event.user.UserEvent;

public final class Runtimes implements EventHandler {

  private static final Object UNKNOWN_RUNTIME = "unknown";

  private final Reference2LongMap<Class<?>> events;

  private long lastEventTime;

  public Runtimes() {
    events = new Reference2LongOpenHashMap<>();
    lastEventTime = Long.MIN_VALUE;
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof LifecycleEvent) {
      events.put(event.getClass(), event.timestamp());
    } else if (event instanceof UserEvent) {
      lastEventTime = Math.max(lastEventTime, event.timestamp());
    }
  }

  @Override
  public void onComplete(Results results) {
    results
        .withScope("runtime")
        .put("createUsers", getRuntime(CreateUsersStart.class, CreateUsersEnd.class))
        .put("sendContacts", getRuntime(SendContactsStart.class, SendContactsEnd.class))
        .put("sendScores", getRuntime(SendRiskScoresStart.class, SendRiskScoresEnd.class))
        .put("riskPropagation", getRuntime(RiskPropagationStart.class, RiskPropagationEnd.class))
        .put("messagePassing", messagePassingRuntime());
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

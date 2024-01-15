package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import java.util.stream.Stream;
import sharetrace.analysis.results.Results;
import sharetrace.logging.event.CreateUsersEnd;
import sharetrace.logging.event.CreateUsersStart;
import sharetrace.logging.event.Event;
import sharetrace.logging.event.RiskPropagationEnd;
import sharetrace.logging.event.RiskPropagationEvent;
import sharetrace.logging.event.RiskPropagationStart;
import sharetrace.logging.event.SendContactsEnd;
import sharetrace.logging.event.SendContactsStart;
import sharetrace.logging.event.SendRiskScoresEnd;
import sharetrace.logging.event.SendRiskScoresStart;
import sharetrace.logging.event.UserEvent;

public final class Runtimes implements EventHandler {

  private static final Object UNKNOWN_RUNTIME = "unknown";

  private final Object2LongMap<Class<?>> events;

  private long lastEventTime;

  public Runtimes() {
    events = new Object2LongOpenHashMap<>();
    lastEventTime = Long.MIN_VALUE;
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof RiskPropagationEvent) {
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
    return isLogged(start, end)
        ? Math.subtractExact(events.getLong(end), events.getLong(start))
        : UNKNOWN_RUNTIME;
  }

  private Object messagePassingRuntime() {
    var start = SendRiskScoresStart.class;
    return isLogged(start)
        ? Math.subtractExact(lastEventTime, events.getLong(start))
        : UNKNOWN_RUNTIME;
  }

  private boolean isLogged(Class<?>... types) {
    return Stream.of(types).allMatch(events::containsKey);
  }
}

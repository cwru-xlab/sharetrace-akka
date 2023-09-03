package sharetrace.analysis.handler;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
import sharetrace.util.Instants;

public final class Runtimes implements EventHandler {

  private static final Object UNKNOWN_RUNTIME = "unknown";
  private final Map<Class<?>, Instant> events;
  private Instant lastEvent;

  public Runtimes() {
    events = new HashMap<>();
    lastEvent = Instant.MIN;
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof RiskPropagationEvent) events.put(event.getClass(), event.timestamp());
    else if (event instanceof UserEvent) lastEvent = Instants.max(lastEvent, event.timestamp());
  }

  @Override
  public void onComplete(Results results) {
    results
        .withScope("runtime")
        .put("createUsers", getRuntime(CreateUsersStart.class, CreateUsersEnd.class))
        .put("sendContacts", getRuntime(SendContactsStart.class, SendContactsEnd.class))
        .put("sendContacts", getRuntime(SendRiskScoresStart.class, SendRiskScoresEnd.class))
        .put("riskPropagation", getRuntime(RiskPropagationStart.class, RiskPropagationEnd.class))
        .put("messagePassing", messagePassingRuntime());
  }

  private Object getRuntime(Class<?> start, Class<?> end) {
    return isLogged(start, end)
        ? Duration.between(events.get(start), events.get(end))
        : UNKNOWN_RUNTIME;
  }

  private Object messagePassingRuntime() {
    var start = SendContactsStart.class;
    return lastEvent != Instant.MIN && isLogged(start)
        ? Duration.between(events.get(start), lastEvent)
        : UNKNOWN_RUNTIME;
  }

  private boolean isLogged(Class<?>... types) {
    return Arrays.stream(types).allMatch(events::containsKey);
  }
}

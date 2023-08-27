package sharetrace.analysis.handler;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import sharetrace.analysis.collector.ResultsCollector;
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

  private Instant lastUserEvent;

  public Runtimes() {
    events = new HashMap<>();
    lastUserEvent = Instant.MIN;
  }

  @Override
  public void onNext(Event event) {
    if (event instanceof RiskPropagationEvent) {
      events.put(event.getClass(), event.timestamp());
    } else if (event instanceof UserEvent) {
      lastUserEvent = Instants.max(lastUserEvent, event.timestamp());
    }
  }

  @Override
  public void onComplete(ResultsCollector collector) {
    collector
        .withPrefix("runtime")
        .put("createUsers", getRuntime(CreateUsersStart.class, CreateUsersEnd.class))
        .put("sendContacts", getRuntime(SendContactsStart.class, SendContactsEnd.class))
        .put("sendContacts", getRuntime(SendRiskScoresStart.class, SendRiskScoresEnd.class))
        .put("riskPropagation", getRuntime(RiskPropagationStart.class, RiskPropagationEnd.class))
        .put("messagePassing", messagePassingRuntime());
  }

  private Object getRuntime(Class<?> startEvent, Class<?> endEvent) {
    if (isLogged(startEvent, endEvent)) {
      var start = events.get(startEvent);
      var end = events.get(endEvent);
      return Duration.between(start, end);
    } else {
      return UNKNOWN_RUNTIME;
    }
  }

  private Object messagePassingRuntime() {
    var startEvent = SendContactsStart.class;
    if (isLogged(startEvent)) {
      var start = events.get(startEvent);
      return Duration.between(start, lastUserEvent);
    } else {
      return UNKNOWN_RUNTIME;
    }
  }

  private boolean isLogged(Class<?>... eventTypes) {
    return Arrays.stream(eventTypes).allMatch(events::containsKey);
  }
}

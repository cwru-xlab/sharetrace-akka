package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import sharetrace.analysis.model.MessagePassingRuntime;
import sharetrace.analysis.model.RiskPropagationRuntime;
import sharetrace.analysis.model.Runtime;
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

  private final Map<Class<?>, Instant> events;

  private Instant lastUserEvent;

  public Runtimes() {
    events = new Object2ObjectOpenHashMap<>();
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
  public void onComplete() {
    var runtimes =
        new Runtime[] {
          createUsersRuntime(),
          sendContactsRuntime(),
          sendRiskScoresRuntime(),
          riskPropagationRuntime(),
          messagePassingRuntime()
        };
  }

  private Runtime createUsersRuntime() {
    return getRuntime(CreateUsersStart.class, CreateUsersEnd.class);
  }

  private Runtime sendContactsRuntime() {
    return getRuntime(SendContactsStart.class, SendContactsEnd.class);
  }

  private Runtime sendRiskScoresRuntime() {
    return getRuntime(SendRiskScoresStart.class, SendRiskScoresEnd.class);
  }

  private Runtime riskPropagationRuntime() {
    return getRuntime(RiskPropagationStart.class, RiskPropagationEnd.class);
  }

  private Runtime messagePassingRuntime() {
    var notLogged = notLogged(SendContactsStart.class);
    if (notLogged.isEmpty()) {
      var start = events.get(SendContactsStart.class);
      return new MessagePassingRuntime(Duration.between(start, lastUserEvent));
    } else {
      throw missingEventsException(notLogged);
    }
  }

  private Runtime getRuntime(Class<?> startEvent, Class<?> endEvent) {
    var notLogged = notLogged(startEvent, endEvent);
    if (notLogged.isEmpty()) {
      var start = events.get(startEvent);
      var end = events.get(endEvent);
      return new RiskPropagationRuntime(Duration.between(start, end));
    } else {
      throw missingEventsException(notLogged);
    }
  }

  private List<Class<?>> notLogged(Class<?>... evenTypes) {
    return Arrays.stream(evenTypes).filter(Predicate.not(events::containsKey)).toList();
  }

  private RuntimeException missingEventsException(Collection<Class<?>> events) {
    return new IllegalStateException("Expected events were not logged: " + events);
  }
}

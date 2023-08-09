package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import sharetrace.analysis.model.CreateUsersRuntime;
import sharetrace.analysis.model.MessagePassingRuntime;
import sharetrace.analysis.model.RiskPropagationRuntime;
import sharetrace.analysis.model.Runtime;
import sharetrace.analysis.model.SendContactsRuntime;
import sharetrace.analysis.model.SendRiskScoresRuntime;
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
    return getRuntime(CreateUsersStart.class, CreateUsersEnd.class, CreateUsersRuntime::new);
  }

  private Runtime sendContactsRuntime() {
    return getRuntime(SendContactsStart.class, SendContactsEnd.class, SendContactsRuntime::new);
  }

  private Runtime sendRiskScoresRuntime() {
    return getRuntime(
        SendRiskScoresStart.class, SendRiskScoresEnd.class, SendRiskScoresRuntime::new);
  }

  private Runtime riskPropagationRuntime() {
    return getRuntime(
        RiskPropagationStart.class, RiskPropagationEnd.class, RiskPropagationRuntime::new);
  }

  private Runtime messagePassingRuntime() {
    var startEvent = SendContactsStart.class;
    if (isLogged(startEvent)) {
      var start = events.get(startEvent);
      return new MessagePassingRuntime(Duration.between(start, lastUserEvent));
    } else {
      return UnknownRuntime.INSTANCE;
    }
  }

  private Runtime getRuntime(
      Class<?> startEvent, Class<?> endEvent, Function<Duration, Runtime> factory) {
    if (isLogged(startEvent, endEvent)) {
      var start = events.get(startEvent);
      var end = events.get(endEvent);
      return factory.apply(Duration.between(start, end));
    } else {
      return UnknownRuntime.INSTANCE;
    }
  }

  private boolean isLogged(Class<?>... evenTypes) {
    return Arrays.stream(evenTypes).allMatch(events::containsKey);
  }
}

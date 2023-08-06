package sharetrace.analysis.handler;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import sharetrace.analysis.model.CreateUsersRuntime;
import sharetrace.analysis.model.MessagePassingRuntime;
import sharetrace.analysis.model.RiskPropagationRuntime;
import sharetrace.analysis.model.Runtime;
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
    var runtimes = new ObjectArrayList<>();
    runtimes.add(createUsersRuntime());
    runtimes.add(sendContactsRuntime());
    runtimes.add(sendRiskScoresRuntime());
    runtimes.add(riskPropagationRuntime());
    runtimes.add(messagePassingRuntime());
    runtimes.trim();
  }

  private Runtime createUsersRuntime() {
    var start = events.get(CreateUsersStart.class);
    var end = events.get(CreateUsersEnd.class);
    return new CreateUsersRuntime(Duration.between(start, end));
  }

  private Runtime sendContactsRuntime() {
    var start = events.get(SendContactsStart.class);
    var end = events.get(SendContactsEnd.class);
    return new CreateUsersRuntime(Duration.between(start, end));
  }

  private Runtime sendRiskScoresRuntime() {
    var start = events.get(SendRiskScoresStart.class);
    var end = events.get(SendRiskScoresEnd.class);
    return new SendRiskScoresRuntime(Duration.between(start, end));
  }

  private Runtime riskPropagationRuntime() {
    var start = events.get(RiskPropagationStart.class);
    var end = events.get(RiskPropagationEnd.class);
    return new RiskPropagationRuntime(Duration.between(start, end));
  }

  private Runtime messagePassingRuntime() {
    var start = events.get(SendContactsStart.class);
    return new MessagePassingRuntime(Duration.between(start, lastUserEvent));
  }
}

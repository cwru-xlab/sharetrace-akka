package org.sharetrace.experiment;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.immutables.value.Value;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.events.ContactEvent;
import org.sharetrace.logging.events.ContactsRefreshEvent;
import org.sharetrace.logging.events.CurrentRefreshEvent;
import org.sharetrace.logging.events.ReceiveEvent;
import org.sharetrace.logging.events.SendCachedEvent;
import org.sharetrace.logging.events.SendCurrentEvent;
import org.sharetrace.logging.events.UpdateEvent;
import org.sharetrace.logging.metrics.GraphCycles;
import org.sharetrace.logging.metrics.GraphEccentricity;
import org.sharetrace.logging.metrics.GraphScores;
import org.sharetrace.logging.metrics.GraphSize;
import org.sharetrace.logging.metrics.GraphTopology;
import org.sharetrace.logging.metrics.MsgPassingRuntime;
import org.sharetrace.logging.metrics.RiskPropRuntime;
import org.sharetrace.logging.metrics.SendContactsRuntime;
import org.sharetrace.logging.metrics.SendScoresRuntime;
import org.sharetrace.logging.settings.ExperimentSettings;

@SuppressWarnings("DefaultAnnotationParam")
@Value.Immutable(copy = true)
abstract class BaseExperimentContext implements AbstractExperimentContext {

  public static ExperimentContext create() {
    return ExperimentContext.builder().build();
  }

  @Override
  @Value.Default
  public Instant refTime() {
    return clock().instant();
  }

  @Override
  @Value.Default
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Override
  @Value.Default
  public long seed() {
    return ThreadLocalRandom.current().nextLong();
  }

  @Override
  @Value.Default
  @SuppressWarnings("immutables:untype")
  public Set<Class<? extends Loggable>> loggable() {
    return Set.of(
        // Events
        ContactEvent.class,
        ContactsRefreshEvent.class,
        CurrentRefreshEvent.class,
        ReceiveEvent.class,
        SendCachedEvent.class,
        SendCurrentEvent.class,
        UpdateEvent.class,
        // Metrics
        GraphCycles.class,
        GraphEccentricity.class,
        GraphScores.class,
        GraphSize.class,
        SendScoresRuntime.class,
        SendContactsRuntime.class,
        RiskPropRuntime.class,
        MsgPassingRuntime.class,
        GraphTopology.class,
        // Settings
        ExperimentSettings.class);
  }
}

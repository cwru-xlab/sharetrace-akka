package org.sharetrace.experiment;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.immutables.value.Value;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.event.ContactEvent;
import org.sharetrace.logging.event.ContactsRefreshEvent;
import org.sharetrace.logging.event.CurrentRefreshEvent;
import org.sharetrace.logging.event.ReceiveEvent;
import org.sharetrace.logging.event.SendCachedEvent;
import org.sharetrace.logging.event.SendCurrentEvent;
import org.sharetrace.logging.event.UpdateEvent;
import org.sharetrace.logging.metric.CreateUsersRuntime;
import org.sharetrace.logging.metric.GraphCycles;
import org.sharetrace.logging.metric.GraphEccentricity;
import org.sharetrace.logging.metric.GraphScores;
import org.sharetrace.logging.metric.GraphSize;
import org.sharetrace.logging.metric.GraphTopology;
import org.sharetrace.logging.metric.MsgPassingRuntime;
import org.sharetrace.logging.metric.RiskPropRuntime;
import org.sharetrace.logging.metric.SendContactsRuntime;
import org.sharetrace.logging.metric.SendScoresRuntime;
import org.sharetrace.logging.setting.ExperimentSettings;

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
        GraphTopology.class,
        CreateUsersRuntime.class,
        SendScoresRuntime.class,
        SendContactsRuntime.class,
        RiskPropRuntime.class,
        MsgPassingRuntime.class,
        // Settings
        ExperimentSettings.class);
  }
}

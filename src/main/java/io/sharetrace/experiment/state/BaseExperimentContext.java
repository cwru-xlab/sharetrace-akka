package io.sharetrace.experiment.state;

import io.sharetrace.logging.Loggable;
import io.sharetrace.logging.event.ContactEvent;
import io.sharetrace.logging.event.ContactsRefreshEvent;
import io.sharetrace.logging.event.CurrentRefreshEvent;
import io.sharetrace.logging.event.ReceiveEvent;
import io.sharetrace.logging.event.SendCachedEvent;
import io.sharetrace.logging.event.SendCurrentEvent;
import io.sharetrace.logging.event.UpdateEvent;
import io.sharetrace.logging.metric.CreateUsersRuntime;
import io.sharetrace.logging.metric.GraphCycles;
import io.sharetrace.logging.metric.GraphEccentricity;
import io.sharetrace.logging.metric.GraphScores;
import io.sharetrace.logging.metric.GraphSize;
import io.sharetrace.logging.metric.GraphTopology;
import io.sharetrace.logging.metric.MsgPassingRuntime;
import io.sharetrace.logging.metric.RiskPropRuntime;
import io.sharetrace.logging.metric.SendContactsRuntime;
import io.sharetrace.logging.metric.SendScoresRuntime;
import io.sharetrace.logging.setting.ExperimentSettings;
import io.sharetrace.util.Uid;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.immutables.value.Value;

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
    return Uid.ofLong();
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

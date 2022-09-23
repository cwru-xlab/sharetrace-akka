package io.sharetrace.experiment.state;

import io.sharetrace.logging.Loggable;
import io.sharetrace.logging.event.*;
import io.sharetrace.logging.metric.*;
import io.sharetrace.logging.setting.ExperimentSettings;
import io.sharetrace.util.Uid;
import org.immutables.value.Value;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

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
    return Uid.ofInt();
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
        TimeoutEvent.class,
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

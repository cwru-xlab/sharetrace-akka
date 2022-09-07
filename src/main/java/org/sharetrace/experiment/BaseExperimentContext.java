package org.sharetrace.experiment;

import java.time.Clock;
import java.time.Instant;
import java.util.Random;
import java.util.Set;
import org.immutables.value.Value;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.events.ContactEvent;
import org.sharetrace.logging.events.ContactsRefreshEvent;
import org.sharetrace.logging.events.CurrentRefreshEvent;
import org.sharetrace.logging.events.ReceiveEvent;
import org.sharetrace.logging.events.SendCachedEvent;
import org.sharetrace.logging.events.SendCurrentEvent;
import org.sharetrace.logging.events.UpdateEvent;
import org.sharetrace.logging.metrics.CycleMetrics;
import org.sharetrace.logging.metrics.EccentricityMetrics;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.logging.metrics.ScoringMetrics;
import org.sharetrace.logging.metrics.SizeMetrics;
import org.sharetrace.logging.metrics.TopologyMetric;
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
    return new Random().nextLong();
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
        CycleMetrics.class,
        EccentricityMetrics.class,
        ScoringMetrics.class,
        SizeMetrics.class,
        RuntimeMetric.class,
        TopologyMetric.class,
        // Settings
        ExperimentSettings.class);
  }
}

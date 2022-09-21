package io.sharetrace.experiment.config;

import io.sharetrace.util.range.Range;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseRuntimeExperimentConfig extends ExperimentConfig {

  public abstract Range<Integer> numNodes();
}

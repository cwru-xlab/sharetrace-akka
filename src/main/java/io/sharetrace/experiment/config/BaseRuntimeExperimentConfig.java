package io.sharetrace.experiment.config;

import org.immutables.value.Value;

@Value.Immutable
abstract class BaseRuntimeExperimentConfig extends NetworkExperimentConfig {

  public abstract Iterable<Integer> numNodes();
}

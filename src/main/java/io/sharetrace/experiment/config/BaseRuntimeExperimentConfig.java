package io.sharetrace.experiment.config;

import org.immutables.value.Value;

@Value.Immutable
abstract class BaseRuntimeExperimentConfig extends ExperimentConfig {

  public abstract Iterable<Integer> numNodes();

  @Value.Default
  public int numNetworks() {
    return 1;
  }
}

package io.sharetrace.experiment.config;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
abstract class BaseRuntimeExperimentConfig extends NetworkExperimentConfig {

    public abstract List<Integer> numNodes();
}

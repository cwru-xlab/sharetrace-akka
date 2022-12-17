package io.sharetrace.experiment.config;

import java.util.List;

import org.immutables.value.Value;

@Value.Immutable
abstract class BaseRuntimeExperimentConfig extends NetworkExperimentConfig {

    public abstract List<Integer> numNodes();
}

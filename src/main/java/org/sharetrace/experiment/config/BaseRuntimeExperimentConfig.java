package org.sharetrace.experiment.config;

import org.immutables.value.Value;
import org.sharetrace.experiment.GraphType;

@Value.Immutable
interface BaseRuntimeExperimentConfig {

  GraphType graphType();

  Iterable<Integer> numNodes();
}

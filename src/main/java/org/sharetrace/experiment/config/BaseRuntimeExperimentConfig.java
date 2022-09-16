package org.sharetrace.experiment.config;

import java.util.Optional;
import org.immutables.value.Value;
import org.sharetrace.experiment.GraphType;

@Value.Immutable
interface BaseRuntimeExperimentConfig {

  Optional<GraphType> graphType();

  Iterable<Integer> numNodes();
}

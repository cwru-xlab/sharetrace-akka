package org.sharetrace.experiment.config;

import java.util.Optional;
import org.immutables.value.Value;
import org.sharetrace.experiment.GraphType;
import org.sharetrace.util.range.IntRange;
import org.sharetrace.util.range.Range;

@Value.Immutable
interface BaseRuntimeExperimentConfig {

  Optional<GraphType> graphType();

  Range<Integer> numNodes();

  @Value.Default
  default Range<Integer> numIterations() {
    return IntRange.single(1);
  }
}

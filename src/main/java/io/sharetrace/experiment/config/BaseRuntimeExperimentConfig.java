package io.sharetrace.experiment.config;

import io.sharetrace.experiment.GraphType;
import io.sharetrace.util.range.IntRange;
import io.sharetrace.util.range.Range;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
interface BaseRuntimeExperimentConfig {

  Optional<GraphType> graphType();

  Range<Integer> numNodes();

  @Value.Default
  default Range<Integer> numIterations() {
    return IntRange.single(1);
  }
}

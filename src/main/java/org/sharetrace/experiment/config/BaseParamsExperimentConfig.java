package org.sharetrace.experiment.config;

import java.util.Optional;
import org.immutables.value.Value;
import org.sharetrace.experiment.GraphType;
import org.sharetrace.util.range.FloatRange;
import org.sharetrace.util.range.Range;

@Value.Immutable
interface BaseParamsExperimentConfig {

  Optional<GraphType> graphType();

  Optional<Integer> numNodes();

  @Value.Default
  default Range<Float> transRates() {
    return FloatRange.of(0.1, 1.0, 0.1);
  }

  @Value.Default
  default Range<Float> sendCoeffs() {
    return FloatRange.of(0.1, 1.1, 0.1);
  }
}

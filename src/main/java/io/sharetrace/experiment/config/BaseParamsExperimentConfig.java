package io.sharetrace.experiment.config;

import io.sharetrace.experiment.GraphType;
import io.sharetrace.util.range.FloatRange;
import io.sharetrace.util.range.Range;
import java.util.Optional;
import org.immutables.value.Value;

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

  @Value.Default
  default int numIterations() {
    return 1;
  }
}

package org.sharetrace.experiment.config;

import org.immutables.value.Value;
import org.sharetrace.experiment.GraphType;
import org.sharetrace.util.range.DoubleRange;
import org.sharetrace.util.range.Range;

@Value.Immutable
interface BaseParamsExperimentConfig {

  GraphType graphType();

  int numNodes();

  @Value.Default
  default Range<Double> transRates() {
    return DoubleRange.of(0.1, 1.0, 0.1);
  }

  @Value.Default
  default Range<Double> sendCoeffs() {
    return DoubleRange.of(0.1, 1.1, 0.1);
  }
}

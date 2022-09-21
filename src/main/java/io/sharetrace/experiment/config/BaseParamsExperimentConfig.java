package io.sharetrace.experiment.config;

import io.sharetrace.util.range.FloatRange;
import io.sharetrace.util.range.Range;
import java.util.OptionalInt;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseParamsExperimentConfig extends ExperimentConfig {

  public abstract OptionalInt numNodes();

  @Value.Default
  public Range<Float> transRates() {
    return FloatRange.of(0.1, 1.0, 0.1);
  }

  @Value.Default
  public Range<Float> sendCoeffs() {
    return FloatRange.of(0.1, 1.1, 0.1);
  }
}

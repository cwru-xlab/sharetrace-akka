package io.sharetrace.experiment.config;

import java.util.List;
import java.util.OptionalInt;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseParamsExperimentConfig extends ExperimentConfig {

  public abstract OptionalInt numNodes();

  @Value.Default
  public Iterable<Float> transRates() {
    return List.of(0.8f);
  }

  @Value.Default
  public Iterable<Float> sendCoeffs() {
    return List.of(0.6f);
  }
}

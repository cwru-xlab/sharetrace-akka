package io.sharetrace.experiment.config;

import java.util.OptionalInt;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseParamsExperimentConfig extends ExperimentConfig {

  public abstract OptionalInt numNodes();

  public abstract Iterable<Float> transRates();

  public abstract Iterable<Float> sendCoeffs();
}

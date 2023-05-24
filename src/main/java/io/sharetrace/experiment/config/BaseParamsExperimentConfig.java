package io.sharetrace.experiment.config;

import java.util.List;
import java.util.OptionalInt;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseParamsExperimentConfig extends NetworkExperimentConfig {

  public abstract OptionalInt users();

  public abstract List<Float> transmissionRates();

  public abstract List<Float> sendCoefficients();
}

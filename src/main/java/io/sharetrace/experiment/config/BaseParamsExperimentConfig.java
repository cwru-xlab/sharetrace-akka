package io.sharetrace.experiment.config;

import org.immutables.value.Value;

import java.util.List;
import java.util.OptionalInt;

@Value.Immutable
abstract class BaseParamsExperimentConfig extends NetworkExperimentConfig {

    public abstract OptionalInt numNodes();

    public abstract List<Float> transRates();

    public abstract List<Float> sendCoeffs();
}

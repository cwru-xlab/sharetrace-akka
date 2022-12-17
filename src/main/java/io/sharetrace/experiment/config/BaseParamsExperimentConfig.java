package io.sharetrace.experiment.config;

import java.util.List;
import java.util.OptionalInt;

import org.immutables.value.Value;

@Value.Immutable
abstract class BaseParamsExperimentConfig extends NetworkExperimentConfig {

    public abstract OptionalInt numNodes();

    public abstract List<Float> transRates();

    public abstract List<Float> sendCoeffs();
}

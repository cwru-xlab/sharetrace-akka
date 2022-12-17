package io.sharetrace.experiment.config;

import io.sharetrace.experiment.data.Dataset;
import io.sharetrace.experiment.state.DatasetContext;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseNoiseExperimentConfig extends NetworkExperimentConfig {

    public abstract Optional<Function<DatasetContext, Dataset>> datasetFactory();

    public abstract List<RealDistribution> noises();
}

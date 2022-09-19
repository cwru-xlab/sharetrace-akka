package io.sharetrace.experiment.config;

import io.sharetrace.data.Dataset;
import io.sharetrace.experiment.GraphType;
import io.sharetrace.experiment.state.DatasetContext;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;

@Value.Immutable
interface BaseNoiseExperimentConfig {

  Optional<GraphType> graphType();

  Optional<Function<DatasetContext, Dataset>> datasetFactory();

  Iterable<RealDistribution> noises();

  @Value.Default
  default int numIterations() {
    return 1;
  }
}

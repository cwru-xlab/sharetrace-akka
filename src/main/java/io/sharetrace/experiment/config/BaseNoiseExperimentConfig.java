package io.sharetrace.experiment.config;

import io.sharetrace.data.Dataset;
import io.sharetrace.experiment.GraphType;
import io.sharetrace.experiment.state.DatasetContext;
import io.sharetrace.util.range.IntRange;
import io.sharetrace.util.range.Range;
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
  default Range<Integer> numIterations() {
    return IntRange.single(1);
  }
}

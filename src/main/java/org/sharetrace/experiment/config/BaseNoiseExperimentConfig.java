package org.sharetrace.experiment.config;

import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;
import org.sharetrace.data.Dataset;
import org.sharetrace.experiment.GraphType;
import org.sharetrace.experiment.state.DatasetContext;
import org.sharetrace.util.range.IntRange;
import org.sharetrace.util.range.Range;

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

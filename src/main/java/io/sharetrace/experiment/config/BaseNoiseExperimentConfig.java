package io.sharetrace.experiment.config;

import io.sharetrace.data.Dataset;
import io.sharetrace.experiment.GraphType;
import io.sharetrace.experiment.state.DatasetContext;
import io.sharetrace.util.Checks;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseNoiseExperimentConfig {

  public abstract Optional<GraphType> graphType();

  public abstract Optional<Function<DatasetContext, Dataset>> datasetFactory();

  public abstract Iterable<RealDistribution> noises();

  @Value.Default
  public int numIterations() {
    return 1;
  }

  @Value.Check
  protected void check() {
    Checks.isAtLeast(numIterations(), 1, "numIterations");
  }
}

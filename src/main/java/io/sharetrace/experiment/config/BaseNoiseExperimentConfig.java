package io.sharetrace.experiment.config;

import com.google.common.collect.Range;
import io.sharetrace.data.Dataset;
import io.sharetrace.experiment.GraphType;
import io.sharetrace.experiment.state.DatasetContext;
import io.sharetrace.util.Checks;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseNoiseExperimentConfig extends ExperimentConfig {

  public static final int MIN_NETWORKS = 1;

  private static final Range<Integer> NETWORKS_RANGE = Range.atLeast(MIN_NETWORKS);

  private static final String NUM_NETWORKS = "numNetworks";

  public abstract Optional<GraphType> graphType();

  @Value.Check
  protected void check() {
    super.check();
    Checks.inRange(numNetworks(), NETWORKS_RANGE, NUM_NETWORKS);
  }

  @Value.Default
  public int numNetworks() {
    return MIN_NETWORKS;
  }

  public abstract Optional<Function<DatasetContext, Dataset>> datasetFactory();

  public abstract Iterable<RealDistribution> noises();
}

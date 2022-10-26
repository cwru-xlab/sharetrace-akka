package io.sharetrace.experiment.config;

import com.google.common.collect.Range;
import io.sharetrace.util.Checks;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseRuntimeExperimentConfig extends ExperimentConfig {

  public static final int MIN_NETWORKS = 1;

  private static final Range<Integer> NETWORKS_RANGE = Range.atLeast(MIN_NETWORKS);

  private static final String NUM_NETWORKS = "numNetworks";

  public abstract Iterable<Integer> numNodes();

  @Value.Check
  protected void check() {
    super.check();
    Checks.inRange(numNetworks(), NETWORKS_RANGE, NUM_NETWORKS);
  }

  @Value.Default
  public int numNetworks() {
    return MIN_NETWORKS;
  }
}

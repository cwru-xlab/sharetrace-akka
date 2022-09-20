package io.sharetrace.experiment.config;

import io.sharetrace.experiment.GraphType;
import io.sharetrace.util.Checks;
import io.sharetrace.util.range.Range;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseRuntimeExperimentConfig {

  public abstract Optional<GraphType> graphType();

  public abstract Range<Integer> numNodes();

  @Value.Default
  public int numIterations() {
    return 1;
  }

  @Value.Check
  protected void check() {
    Checks.isAtLeast(numIterations(), 1, "numIterations");
  }
}

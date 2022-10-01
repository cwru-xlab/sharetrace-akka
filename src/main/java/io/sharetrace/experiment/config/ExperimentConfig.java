package io.sharetrace.experiment.config;

import io.sharetrace.experiment.GraphType;
import io.sharetrace.util.Checks;
import java.util.Optional;
import org.immutables.value.Value;

abstract class ExperimentConfig {

  public abstract Optional<GraphType> graphType();

  @Value.Check
  protected void check() {
    Checks.isAtLeast(numIterations(), 1, "numIterations");
  }

  @Value.Default
  public int numIterations() {
    return 1;
  }
}

package io.sharetrace.experiment.config;

import io.sharetrace.experiment.GraphType;
import io.sharetrace.util.Checks;
import java.nio.file.Path;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseFileExperimentConfig {

  public abstract Optional<GraphType> graphType();

  public abstract Optional<Path> path();

  @Value.Default
  public int numIterations() {
    return 1;
  }

  @Value.Check
  protected void check() {
    Checks.isAtLeast(numIterations(), 1, "numIterations");
  }
}

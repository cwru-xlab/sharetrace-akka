package io.sharetrace.experiment.config;

import io.sharetrace.experiment.GraphType;
import java.nio.file.Path;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
interface BaseFileExperimentConfig {

  @Value.Default
  default int numIterations() {
    return 1;
  }

  Optional<GraphType> graphType();

  Optional<Path> path();
}

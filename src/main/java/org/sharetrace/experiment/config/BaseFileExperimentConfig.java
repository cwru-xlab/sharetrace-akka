package org.sharetrace.experiment.config;

import java.nio.file.Path;
import org.immutables.value.Value;
import org.sharetrace.experiment.GraphType;

@Value.Immutable
interface BaseFileExperimentConfig {

  @Value.Default
  default int numIterations() {
    return 1;
  }

  GraphType graphType();

  Path path();
}
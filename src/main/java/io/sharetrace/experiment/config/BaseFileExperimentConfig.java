package io.sharetrace.experiment.config;

import java.nio.file.Path;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseFileExperimentConfig extends ExperimentConfig {

  public abstract Optional<Path> path();
}

package io.sharetrace.experiment.config;

import org.immutables.value.Value;

import java.nio.file.Path;
import java.util.Optional;

@Value.Immutable
abstract class BaseFileExperimentConfig extends ExperimentConfig {

    public abstract Optional<Path> path();
}

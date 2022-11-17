package io.sharetrace.experiment;

import io.sharetrace.experiment.config.FileExperimentConfig;
import io.sharetrace.experiment.config.NoiseExperimentConfig;
import io.sharetrace.experiment.config.ParamsExperimentConfig;
import io.sharetrace.experiment.config.RuntimeExperimentConfig;

public final class Experiments {

  private Experiments() {}

  public static Experiment<NoiseExperimentConfig> noise() {
    return new NoiseExperiment();
  }

  public static Experiment<ParamsExperimentConfig> params() {
    return new ParamsExperiment();
  }

  public static Experiment<RuntimeExperimentConfig> runtime() {
    return new RuntimeExperiment();
  }

  public static Experiment<FileExperimentConfig> file() {
    return new FileExperiment();
  }
}

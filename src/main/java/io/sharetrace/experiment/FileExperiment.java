package io.sharetrace.experiment;

import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.experiment.config.FileExperimentConfig;
import io.sharetrace.experiment.data.FileDataset;
import io.sharetrace.experiment.state.Context;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.State;
import io.sharetrace.graph.GraphType;
import java.nio.file.Path;

public final class FileExperiment extends Experiment<FileExperimentConfig> {

  private FileExperiment() {}

  public static FileExperiment create() {
    return new FileExperiment();
  }

  /** Evaluates {@link RiskPropagation} on a given {@link FileDataset} 1 or more times. */
  @Override
  public void run(FileExperimentConfig config, State state) {
    state.run(config.numIterations());
  }

  @Override
  public State newDefaultState(Context ctx, FileExperimentConfig config) {
    GraphType graphType = getProperty(config.graphType(), "graphType");
    Path path = getProperty(config.path(), "path");
    return State.builder(ctx)
        .graphType(graphType)
        .dataset(context -> Defaults.fileDataset(context, path))
        .build();
  }
}

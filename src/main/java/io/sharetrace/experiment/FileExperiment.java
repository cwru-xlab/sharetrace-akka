package io.sharetrace.experiment;

import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.data.FileDataset;
import io.sharetrace.experiment.config.FileExperimentConfig;
import io.sharetrace.experiment.state.Context;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.State;
import java.nio.file.Path;

public final class FileExperiment extends Experiment<FileExperimentConfig> {

  private static final FileExperiment INSTANCE = new FileExperiment();

  private FileExperiment() {}

  public static FileExperiment instance() {
    return INSTANCE;
  }

  /** Evaluates {@link RiskPropagation} on a given {@link FileDataset} 1 or more times. */
  @Override
  public void run(FileExperimentConfig config, State initialState) {
    initialState.run(config.numIterations());
  }

  @Override
  public State newDefaultState(FileExperimentConfig config) {
    return newDefaultState(Defaults.context(), config);
  }

  @Override
  public State newDefaultState(Context context, FileExperimentConfig config) {
    GraphType graphType = getProperty(config.graphType(), "graphType");
    Path path = getProperty(config.path(), "path");
    return State.builder(context)
        .graphType(graphType)
        .dataset(ctx -> Defaults.fileDataset(ctx, path))
        .build();
  }
}

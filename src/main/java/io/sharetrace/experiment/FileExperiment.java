package io.sharetrace.experiment;

import io.sharetrace.experiment.config.FileExperimentConfig;
import io.sharetrace.experiment.state.Context;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.State;
import io.sharetrace.graph.GraphType;
import java.nio.file.Path;

public final class FileExperiment extends Experiment<FileExperimentConfig> {

  @Override
  public void run(FileExperimentConfig config, State state) {
    state.run(config.iterations());
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

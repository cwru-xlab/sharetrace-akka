package org.sharetrace.experiment;

import java.nio.file.Path;
import org.sharetrace.experiment.config.FileExperimentConfig;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentContext;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.util.range.IntRange;

public final class FileExperiment implements Experiment<FileExperimentConfig> {

  private static final FileExperiment INSTANCE = new FileExperiment();

  private FileExperiment() {}

  public static FileExperiment instance() {
    return INSTANCE;
  }

  @Override
  public void run(ExperimentState initialState, FileExperimentConfig config) {
    IntRange.of(config.numIterations()).forEach(x -> initialState.withNewId().run());
  }

  @Override
  public ExperimentState newDefaultState(FileExperimentConfig config) {
    GraphType graphType = getProperty(config.graphType(), "graphType");
    Path path = getProperty(config.path(), "path");
    return ExperimentState.builder(ExperimentContext.create())
        .graphType(graphType)
        .dataset(ctx -> Defaults.fileDataset(ctx, path))
        .build();
  }
}

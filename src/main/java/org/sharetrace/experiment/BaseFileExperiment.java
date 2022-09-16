package org.sharetrace.experiment;

import java.nio.file.Path;
import org.immutables.value.Value;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.util.range.IntRange;

@Value.Immutable
abstract class BaseFileExperiment implements Experiment {

  public static FileExperiment create() {
    return FileExperiment.builder().build();
  }

  public static ExperimentState newDefaultState(GraphType graphType, Path path) {
    return ExperimentState.builder(ExperimentContext.create())
        .graphType(graphType)
        .dataset(ctx -> Defaults.fileDataset(ctx, path))
        .build();
  }

  public void runWithDefaults(GraphType graphType, Path path) {
    run(newDefaultState(graphType, path));
  }

  @Override
  public void run(ExperimentState initialState) {
    IntRange.of(numIterations()).forEach(x -> initialState.withNewId().run());
  }

  @Value.Parameter
  @Value.Default
  protected int numIterations() {
    return 1;
  }
}

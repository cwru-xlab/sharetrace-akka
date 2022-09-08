package org.sharetrace.experiment;

import java.nio.file.Path;
import org.immutables.value.Value;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.util.Range;

@Value.Immutable
abstract class BaseFileExperiment implements Experiment {

  public static FileExperiment create() {
    return FileExperiment.builder().build();
  }

  private static ExperimentState defaultFileState(GraphType graphType, Path path) {
    return ExperimentState.builder(ExperimentContext.create())
        .graphType(graphType)
        .dataset(ctx -> Defaults.fileDataset(ctx, path))
        .build();
  }

  public void runWithDefaults(GraphType graphType, Path path) {
    run(defaultFileState(graphType, path));
  }

  @Override
  public void run(ExperimentState initialState) {
    Range.of(numIterations()).forEach(x -> initialState.withNewId().run());
  }

  @Value.Parameter
  @Value.Default
  protected int numIterations() {
    return 1;
  }
}

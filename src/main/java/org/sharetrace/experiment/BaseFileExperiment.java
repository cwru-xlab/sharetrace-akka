package org.sharetrace.experiment;

import java.nio.file.Path;
import org.immutables.value.Value;
import org.sharetrace.experiment.FileExperiment.Inputs;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.util.range.IntRange;

@Value.Immutable
@Value.Enclosing
abstract class BaseFileExperiment implements Experiment<Inputs> {

  public static FileExperiment create() {
    return FileExperiment.builder().build();
  }

  @Override
  public void run(ExperimentState initialState) {
    IntRange.of(numIterations()).forEach(x -> initialState.withNewId().run());
  }

  @Override
  public ExperimentState newDefaultState(Inputs inputs) {
    return ExperimentState.builder(ExperimentContext.create())
        .graphType(inputs.graphType())
        .dataset(ctx -> Defaults.fileDataset(ctx, inputs.path()))
        .build();
  }

  @Value.Parameter
  @Value.Default
  protected int numIterations() {
    return 1;
  }

  @Value.Immutable
  interface BaseInputs {

    @Value.Parameter
    GraphType graphType();

    @Value.Parameter
    Path path();
  }
}

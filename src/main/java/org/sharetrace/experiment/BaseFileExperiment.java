package org.sharetrace.experiment;

import java.util.stream.IntStream;
import org.immutables.value.Value;
import org.sharetrace.experiment.state.ExperimentState;

@Value.Immutable
abstract class BaseFileExperiment implements Experiment {

  @Override
  public void run(ExperimentState initialState) {
    IntStream.of(numIterations()).forEach(x -> initialState.run());
  }

  @Value.Parameter
  protected abstract int numIterations();
}

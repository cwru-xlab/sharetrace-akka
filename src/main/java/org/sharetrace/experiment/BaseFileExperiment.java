package org.sharetrace.experiment;

import org.immutables.value.Value;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.util.Range;

@Value.Immutable
abstract class BaseFileExperiment implements Experiment {

  @Override
  public void run(ExperimentState initialState) {
    // Only need to generate a new state ID.
    Range.of(numIterations()).forEach(x -> initialState.toBuilder().build().run());
  }

  @Value.Parameter
  protected abstract int numIterations();
}

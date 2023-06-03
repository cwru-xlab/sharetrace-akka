package io.sharetrace.experiment;

import org.immutables.value.Value;

@Value.Immutable
abstract class BaseStandardExperiment<T> extends AbstractExperiment<T> {

  @Override
  public void run(ExperimentState<T> state) {
    state.run(iterations());
  }
}

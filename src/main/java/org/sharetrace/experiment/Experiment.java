package org.sharetrace.experiment;

import java.util.function.UnaryOperator;
import org.sharetrace.experiment.state.ExperimentState;

public interface Experiment<T> {

  void run(ExperimentState initialState);

  default void runWithDefaults(T inputs) {
    runFromDefaults(UnaryOperator.identity(), inputs);
  }

  default void runFromDefaults(UnaryOperator<ExperimentState> overrideDefaults, T inputs) {
    run(overrideDefaults.apply(newDefaultState(inputs)));
  }

  ExperimentState newDefaultState(T inputs);
}

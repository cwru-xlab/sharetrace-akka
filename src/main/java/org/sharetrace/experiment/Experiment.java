package org.sharetrace.experiment;

import java.util.function.UnaryOperator;
import org.sharetrace.experiment.state.ExperimentState;

public interface Experiment<T> {

  void run(ExperimentState initialState, T config);

  default void runWithDefaults(T config) {
    runFromDefaults(UnaryOperator.identity(), config);
  }

  default void runFromDefaults(UnaryOperator<ExperimentState> overrideDefaults, T config) {
    run(overrideDefaults.apply(newDefaultState(config)), config);
  }

  ExperimentState newDefaultState(T config);
}

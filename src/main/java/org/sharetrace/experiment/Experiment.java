package org.sharetrace.experiment;

import java.util.Optional;
import java.util.function.UnaryOperator;
import org.sharetrace.experiment.config.MissingConfigException;
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

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  default <R> R getProperty(Optional<R> property, String name) {
    return property.orElseThrow(() -> new MissingConfigException(name));
  }
}

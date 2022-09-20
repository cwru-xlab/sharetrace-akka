package io.sharetrace.experiment;

import io.sharetrace.experiment.config.MissingConfigException;
import io.sharetrace.experiment.state.ExperimentState;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;

public interface Experiment<T> {

  void run(ExperimentState initialState, T config);

  default void runWithDefaults(T config) {
    runWithDefaults(UnaryOperator.identity(), config);
  }

  default void runWithDefaults(UnaryOperator<ExperimentState> overrideDefaults, T config) {
    run(overrideDefaults.apply(newDefaultState(config)), config);
  }

  ExperimentState newDefaultState(T config);

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  default <R> R getProperty(Optional<R> property, String name) {
    return property.orElseThrow(() -> new MissingConfigException(name));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  default int getProperty(OptionalInt property, String name) {
    return property.orElseThrow(() -> new MissingConfigException(name));
  }
}

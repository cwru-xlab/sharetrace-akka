package io.sharetrace.experiment;

import io.sharetrace.experiment.config.MissingConfigException;
import io.sharetrace.experiment.state.ExperimentContext;
import io.sharetrace.experiment.state.ExperimentState;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;

public abstract class Experiment<Config> {

  protected Experiment() {}

  public abstract void run(ExperimentState initialState, Config config);

  public void runWithDefaults(Config config) {
    runWithDefaults(UnaryOperator.identity(), config);
  }

  public void runWithDefaults(UnaryOperator<ExperimentState> overrideDefaults, Config config) {
    run(overrideDefaults.apply(newDefaultState(config)), config);
  }

  public abstract ExperimentState newDefaultState(Config config);

  public abstract ExperimentState newDefaultState(ExperimentContext context, Config config);

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  protected <R> R getProperty(Optional<R> property, String name) {
    return property.orElseThrow(() -> new MissingConfigException(name));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  protected int getProperty(OptionalInt property, String name) {
    return property.orElseThrow(() -> new MissingConfigException(name));
  }
}

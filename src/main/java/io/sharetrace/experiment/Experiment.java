package io.sharetrace.experiment;

import io.sharetrace.experiment.state.Context;
import io.sharetrace.experiment.state.State;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;

public abstract class Experiment<Config> {

  protected Experiment() {}

  public void runWithDefaults(Config config) {
    runWithDefaults(config, UnaryOperator.identity());
  }

  public void runWithDefaults(Config config, UnaryOperator<State> overrideDefaults) {
    run(config, overrideDefaults.apply(newDefaultState(config)));
  }

  public abstract void run(Config config, State initialState);

  public abstract State newDefaultState(Config config);

  public abstract State newDefaultState(Context context, Config config);

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  protected <R> R getProperty(Optional<R> property, String name) {
    return property.orElseThrow(() -> new NoSuchElementException(name));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  protected int getProperty(OptionalInt property, String name) {
    return property.orElseThrow(() -> new NoSuchElementException(name));
  }
}

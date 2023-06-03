package io.sharetrace.experiment;

public interface Experiment<T> {

  void run(ExperimentState<T> state);

  static <T> Experiment<T> from(ExperimentState<T> state) {
    return x -> state.run();
  }
}

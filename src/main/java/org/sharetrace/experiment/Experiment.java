package org.sharetrace.experiment;

import org.sharetrace.experiment.state.ExperimentState;

@FunctionalInterface
public interface Experiment {

  void run(ExperimentState initialState);
}

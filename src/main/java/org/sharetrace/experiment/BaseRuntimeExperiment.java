package org.sharetrace.experiment;

import org.immutables.value.Value;
import org.sharetrace.data.SampledDataset;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.util.Range;

@Value.Immutable
abstract class BaseRuntimeExperiment implements Experiment {

  @Override
  public void run(ExperimentState initialState) {
    SampledDataset dataset = (SampledDataset) initialState.dataset();
    for (double n : numNodes()) {
      initialState.toBuilder().dataset(dataset.withNumNodes((int) n)).build().run();
    }
  }

  @Value.Parameter
  protected abstract Range numNodes();
}

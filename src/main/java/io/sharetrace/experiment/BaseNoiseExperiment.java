package io.sharetrace.experiment;

import io.sharetrace.util.DistributedRandom;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseNoiseExperiment<T> extends AbstractExperiment<T> {

  public abstract List<DistributedRandom> noises();

  @Override
  public void run(ExperimentState<T> state) {
    for (int i = 0; i < networks(); i++) {
      state = state.withNewNetwork();
      for (DistributedRandom noise : noises()) {
        state.mapScoreFactory(factory -> factory.cached().noisy(noise)).run(iterations());
      }
    }
  }
}

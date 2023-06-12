package sharetrace.evaluation;

import java.util.List;
import org.immutables.value.Value;
import sharetrace.experiment.AbstractExperiment;
import sharetrace.experiment.ExperimentState;
import sharetrace.util.DistributedRandom;

@Value.Immutable
abstract class BaseNoiseExperiment<K> extends AbstractExperiment<K> {

  public abstract List<DistributedRandom> noises();

  @Override
  public void run(ExperimentState<K> state) {
    for (int i = 0; i < networks(); i++) {
      state = state.withNewNetwork();
      for (DistributedRandom noise : noises()) {
        state.mapScoreFactory(factory -> factory.cached().noisy(noise)).run(iterations());
      }
    }
  }
}

package sharetrace.evaluation;

import org.immutables.value.Value;
import sharetrace.experiment.AbstractExperiment;
import sharetrace.experiment.ExperimentState;

@Value.Immutable
abstract class BaseStandardExperiment<K> extends AbstractExperiment<K> {

  @Override
  public void run(ExperimentState<K> state) {
    state.run(iterations());
  }
}

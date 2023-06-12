package sharetrace.evaluation;

import org.immutables.value.Value;
import sharetrace.experiment.AbstractExperiment;
import sharetrace.experiment.ExperimentState;

@Value.Immutable
abstract class BaseStandardExperiment<T> extends AbstractExperiment<T> {

  @Override
  public void run(ExperimentState<T> state) {
    state.run(iterations());
  }
}

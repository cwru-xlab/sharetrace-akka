package sharetrace.evaluation;

import java.util.List;
import org.immutables.value.Value;
import sharetrace.experiment.AbstractExperiment;
import sharetrace.experiment.ExperimentState;
import sharetrace.graph.TemporalNetworkFactory;

@Value.Immutable
abstract class BaseRuntimeExperiment<T> extends AbstractExperiment<T> {

  public abstract List<TemporalNetworkFactory<T>> networkFactories();

  @Override
  public void run(ExperimentState<T> state) {
    for (TemporalNetworkFactory<T> networkFactory : networkFactories()) {
      for (int i = 0; i < networks(); i++) {
        state.withNetworkFactory(networkFactory).run(iterations());
      }
    }
  }
}

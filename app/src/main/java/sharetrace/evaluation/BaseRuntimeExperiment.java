package sharetrace.evaluation;

import java.util.List;
import org.immutables.value.Value;
import sharetrace.experiment.AbstractExperiment;
import sharetrace.experiment.ExperimentState;
import sharetrace.graph.TemporalNetworkFactory;

@Value.Immutable
abstract class BaseRuntimeExperiment<K> extends AbstractExperiment<K> {

  public abstract List<TemporalNetworkFactory<K>> networkFactories();

  @Override
  public void run(ExperimentState<K> state) {
    for (TemporalNetworkFactory<K> networkFactory : networkFactories()) {
      for (int i = 0; i < networks(); i++) {
        state.withNetworkFactory(networkFactory).run(iterations());
      }
    }
  }
}

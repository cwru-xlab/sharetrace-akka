package io.sharetrace.experiment;

import io.sharetrace.graph.TemporalNetworkFactory;
import java.util.List;
import org.immutables.value.Value;

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

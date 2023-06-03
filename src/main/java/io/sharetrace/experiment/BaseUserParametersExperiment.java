package io.sharetrace.experiment;

import io.sharetrace.experiment.data.RiskScoreFactory;
import io.sharetrace.model.UserParameters;
import java.util.List;
import java.util.function.UnaryOperator;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseUserParametersExperiment<T> extends AbstractExperiment<T> {

  public abstract List<Float> transmissionRates();

  public abstract List<Float> sendCoefficients();

  @Override
  public void run(ExperimentState<T> state) {
    for (int i = 0; i < networks(); i++) {
      state = state.mapScoreFactory(RiskScoreFactory::cached).withNewNetwork();
      for (float tr : transmissionRates()) {
        for (float sc : sendCoefficients()) {
          state.mapUserParameters(mapParams(tr, sc)).run(iterations());
        }
      }
    }
  }

  private UnaryOperator<UserParameters> mapParams(float transmissionRate, float sendCoefficient) {
    return p -> p.withTransmissionRate(transmissionRate).withSendCoefficient(sendCoefficient);
  }
}

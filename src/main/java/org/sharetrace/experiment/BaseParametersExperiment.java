package org.sharetrace.experiment;

import org.immutables.value.Value;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.message.MessageParameters;
import org.sharetrace.util.Range;

@Value.Immutable
abstract class BaseParametersExperiment implements Experiment {

  public static ParametersExperiment create() {
    return ParametersExperiment.builder().build();
  }

  @Override
  public void run(ExperimentState initialState) {
    MessageParameters newParameters;
    for (double tr : transmissionRates()) {
      for (double sc : sendCoefficients()) {
        newParameters =
            initialState
                .messageParameters()
                .withTransmissionRate((float) tr)
                .withSendCoefficient((float) sc);
        initialState.toBuilder().messageParameters(newParameters).build().run();
      }
    }
  }

  @Value.Parameter
  @Value.Default
  protected Range transmissionRates() {
    return Range.of(1, 10, 1, 0.1);
  }

  @Value.Parameter
  @Value.Default
  protected Range sendCoefficients() {
    return Range.of(1, 11, 1, 0.1);
  }
}

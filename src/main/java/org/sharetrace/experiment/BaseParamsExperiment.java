package org.sharetrace.experiment;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.logging.metrics.TopologyMetric;
import org.sharetrace.util.Range;

@Value.Immutable
abstract class BaseParamsExperiment implements Experiment {

  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  public static ParamsExperiment create() {
    return ParamsExperiment.builder().build();
  }

  private static ExperimentContext newDefaultContext() {
    ExperimentContext context = ExperimentContext.create();
    return context.withLoggable(
        context.loggable().stream()
            .filter(Predicate.not(loggable -> loggable.equals(TopologyMetric.class)))
            .collect(Collectors.toUnmodifiableSet()));
  }

  private static ExperimentState newDefaultState(GraphType graphType, int numNodes) {
    return ExperimentState.builder(DEFAULT_CTX)
        .graphType(graphType)
        .dataset(ctx -> Defaults.sampledDataset(ctx, numNodes))
        .build();
  }

  public void runWithDefaults(GraphType graphType, int numNodes) {
    run(newDefaultState(graphType, numNodes));
  }

  @Override
  public void run(ExperimentState initialState) {
    for (double tr : transmissionRates()) {
      for (double sc : sendCoefficients()) {
        initialState.toBuilder()
            .msgParams(
                initialState
                    .msgParams()
                    .withTransmissionRate((float) tr)
                    .withSendCoefficient((float) sc))
            .build()
            .run();
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

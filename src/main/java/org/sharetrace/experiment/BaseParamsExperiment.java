package org.sharetrace.experiment;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.logging.metric.GraphTopology;
import org.sharetrace.util.Ranges;

@Value.Immutable
abstract class BaseParamsExperiment implements Experiment {

  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  public static ParamsExperiment create() {
    return ParamsExperiment.builder().build();
  }

  public static ExperimentState newDefaultState(GraphType graphType, int numNodes) {
    return ExperimentState.builder(DEFAULT_CTX)
        .graphType(graphType)
        .dataset(ctx -> Defaults.sampledDataset(ctx, numNodes))
        .build();
  }

  private static ExperimentContext newDefaultContext() {
    ExperimentContext context = ExperimentContext.create();
    return context.withLoggable(
        context.loggable().stream()
            .filter(Predicate.not(loggable -> loggable.equals(GraphTopology.class)))
            .collect(Collectors.toUnmodifiableSet()));
  }

  public void runWithDefaults(GraphType graphType, int numNodes) {
    run(newDefaultState(graphType, numNodes));
  }

  @Override
  public void run(ExperimentState initialState) {
    for (float tr : transmissionRates()) {
      for (float sc : sendCoefficients()) {
        initialState.toBuilder()
            .msgParams(initialState.msgParams().withTransmissionRate(tr).withSendCoefficient(sc))
            .build()
            .run();
      }
    }
  }

  @Value.Parameter
  @Value.Default
  protected Iterable<Float> transmissionRates() {
    return Ranges.ofFloats(0.1f, 1f, 0.1f);
  }

  @Value.Parameter
  @Value.Default
  protected Iterable<Float> sendCoefficients() {
    return Ranges.ofFloats(0.1f, 1.1f, 0.1f);
  }
}

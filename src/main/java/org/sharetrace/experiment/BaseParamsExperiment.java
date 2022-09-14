package org.sharetrace.experiment;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.logging.metric.GraphTopology;
import org.sharetrace.util.range.DoubleRange;
import org.sharetrace.util.range.Range;

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
    ExperimentContext ctx = ExperimentContext.create();
    return ctx.withLoggable(
        ctx.loggable().stream()
            .filter(Predicate.not(loggable -> loggable.equals(GraphTopology.class)))
            .collect(Collectors.toUnmodifiableSet()));
  }

  public void runWithDefaults(GraphType graphType, int numNodes) {
    run(newDefaultState(graphType, numNodes));
  }

  @Override
  public void run(ExperimentState initialState) {
    for (double tr : transRates()) {
      for (double sc : sendCoeffs()) {
        initialState.toBuilder()
            .msgParams(initialState.msgParams().withTransRate(tr).withSendCoeff(sc))
            .build()
            .run();
      }
    }
  }

  @Value.Parameter
  @Value.Default
  protected Range<Double> transRates() {
    return DoubleRange.of(0.1, 1.0, 0.1);
  }

  @Value.Parameter
  @Value.Default
  protected Range<Double> sendCoeffs() {
    return DoubleRange.of(0.1, 1.1, 0.1);
  }
}

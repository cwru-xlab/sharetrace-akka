package org.sharetrace.experiment;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.immutables.value.Value;
import org.sharetrace.experiment.ParamsExperiment.Inputs;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.logging.metric.GraphTopology;
import org.sharetrace.util.range.DoubleRange;
import org.sharetrace.util.range.Range;

@Value.Immutable
@Value.Enclosing
abstract class BaseParamsExperiment implements Experiment<Inputs> {

  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  public static ParamsExperiment create() {
    return ParamsExperiment.builder().build();
  }

  private static ExperimentContext newDefaultContext() {
    ExperimentContext ctx = ExperimentContext.create();
    return ctx.withLoggable(
        ctx.loggable().stream()
            .filter(Predicate.not(loggable -> loggable.equals(GraphTopology.class)))
            .collect(Collectors.toUnmodifiableSet()));
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

  @Override
  public ExperimentState newDefaultState(Inputs inputs) {
    return ExperimentState.builder(DEFAULT_CTX)
        .graphType(inputs.graphType())
        .dataset(ctx -> Defaults.sampledDataset(ctx, inputs.numNodes()))
        .build();
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

  @Value.Immutable
  interface BaseInputs {

    @Value.Parameter
    GraphType graphType();

    @Value.Parameter
    int numNodes();
  }
}

package org.sharetrace.experiment;

import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.sharetrace.experiment.config.ParamsExperimentConfig;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentContext;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.logging.metric.GraphTopology;

public final class ParamsExperiment implements Experiment<ParamsExperimentConfig> {

  private static final ParamsExperiment INSTANCE = new ParamsExperiment();
  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  private ParamsExperiment() {}

  public static ParamsExperiment instance() {
    return INSTANCE;
  }

  private static ExperimentContext newDefaultContext() {
    ExperimentContext ctx = ExperimentContext.create();
    return ctx.withLoggable(
        ctx.loggable().stream()
            .filter(Predicate.not(loggable -> loggable.equals(GraphTopology.class)))
            .collect(Collectors.toUnmodifiableSet()));
  }

  @Override
  public void run(ExperimentState initialState, ParamsExperimentConfig config) {
    for (double tr : config.transRates()) {
      for (double sc : config.sendCoeffs()) {
        initialState.toBuilder()
            .msgParams(initialState.msgParams().withTransRate(tr).withSendCoeff(sc))
            .build()
            .run();
      }
    }
  }

  @Override
  public ExperimentState newDefaultState(ParamsExperimentConfig config) {
    return ExperimentState.builder(DEFAULT_CTX)
        .graphType(config.graphType())
        .dataset(ctx -> Defaults.sampledDataset(ctx, config.numNodes()))
        .build();
  }
}

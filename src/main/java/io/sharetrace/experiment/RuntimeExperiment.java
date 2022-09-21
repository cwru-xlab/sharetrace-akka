package io.sharetrace.experiment;

import io.sharetrace.data.Dataset;
import io.sharetrace.data.SampledDataset;
import io.sharetrace.experiment.config.RuntimeExperimentConfig;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.ExperimentContext;
import io.sharetrace.experiment.state.ExperimentState;
import io.sharetrace.logging.metric.CreateUsersRuntime;
import io.sharetrace.logging.metric.GraphSize;
import io.sharetrace.logging.metric.MsgPassingRuntime;
import io.sharetrace.logging.metric.RiskPropRuntime;
import io.sharetrace.logging.metric.SendContactsRuntime;
import io.sharetrace.logging.metric.SendScoresRuntime;
import io.sharetrace.logging.setting.ExperimentSettings;
import io.sharetrace.util.range.IntRange;
import java.util.Set;

public final class RuntimeExperiment implements Experiment<RuntimeExperimentConfig> {

  private static final RuntimeExperiment INSTANCE = new RuntimeExperiment();
  private static final int IGNORED = 50;
  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  private RuntimeExperiment() {}

  public static RuntimeExperiment instance() {
    return INSTANCE;
  }

  public static ExperimentContext newDefaultContext() {
    return ExperimentContext.create()
        .withLoggable(
            Set.of(
                GraphSize.class,
                CreateUsersRuntime.class,
                SendScoresRuntime.class,
                SendContactsRuntime.class,
                RiskPropRuntime.class,
                MsgPassingRuntime.class,
                ExperimentSettings.class));
  }

  private static void forEachDataset(ExperimentState state, Dataset dataset) {
    state.toBuilder()
        .dataset(dataset.withNewContactNetwork())
        .userParams(ctx -> Defaults.userParams(ctx.dataset()))
        .build()
        .run();
  }

  @Override
  public void run(ExperimentState initialState, RuntimeExperimentConfig config) {
    for (int n : config.numNodes()) {
      Dataset newDataset = ((SampledDataset) initialState.dataset()).withNumNodes(n);
      IntRange.of(config.numIterations()).forEach(x -> forEachDataset(initialState, newDataset));
    }
  }

  @Override
  public ExperimentState newDefaultState(RuntimeExperimentConfig config) {
    return newDefaultState(DEFAULT_CTX, config);
  }

  @Override
  public ExperimentState newDefaultState(
      ExperimentContext context, RuntimeExperimentConfig config) {
    return ExperimentState.builder(context)
        .graphType(getProperty(config.graphType(), "graphType"))
        .dataset(ctx -> Defaults.sampledDataset(ctx, IGNORED))
        .build();
  }
}
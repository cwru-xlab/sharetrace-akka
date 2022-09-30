package io.sharetrace.experiment;

import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.data.SampledDataset;
import io.sharetrace.experiment.config.RuntimeExperimentConfig;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.ExperimentContext;
import io.sharetrace.experiment.state.ExperimentState;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.logging.metric.CreateUsersRuntime;
import io.sharetrace.logging.metric.GraphSize;
import io.sharetrace.logging.metric.MsgPassingRuntime;
import io.sharetrace.logging.metric.RiskPropRuntime;
import io.sharetrace.logging.metric.SendContactsRuntime;
import io.sharetrace.logging.metric.SendScoresRuntime;
import io.sharetrace.logging.setting.ExperimentSettings;

public final class RuntimeExperiment extends Experiment<RuntimeExperimentConfig> {

  private static final RuntimeExperiment INSTANCE = new RuntimeExperiment();
  private static final int IGNORED = 50;
  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  private RuntimeExperiment() {}

  public static RuntimeExperiment instance() {
    return INSTANCE;
  }

  public static ExperimentContext newDefaultContext() {
    return Defaults.context()
        .withLoggable(
            GraphSize.class,
            CreateUsersRuntime.class,
            SendScoresRuntime.class,
            SendContactsRuntime.class,
            RiskPropRuntime.class,
            MsgPassingRuntime.class,
            ExperimentSettings.class);
  }

  /**
   * Evaluates the runtime performance of {@link RiskPropagation} for a given {@link GraphType}. The
   * same {@link ContactNetwork} is evaluated 1 or more times for each number of nodes.
   */
  @Override
  public void run(ExperimentState initialState, RuntimeExperimentConfig config) {
    SampledDataset dataset = (SampledDataset) initialState.dataset();
    for (int n : config.numNodes()) {
      dataset = dataset.withNumNodes(n);
      for (int iNetwork = 0; iNetwork < config.numNetworks(); iNetwork++) {
        initialState.toBuilder()
            .dataset(dataset.withNewContactNetwork())
            .userParams(ctx -> Defaults.userParams(ctx.dataset()))
            .build()
            .run(config.numIterations());
      }
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

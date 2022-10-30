package io.sharetrace.experiment;

import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.experiment.config.RuntimeExperimentConfig;
import io.sharetrace.experiment.data.Dataset;
import io.sharetrace.experiment.data.SampledDataset;
import io.sharetrace.experiment.state.Context;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.State;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.graph.GraphType;
import io.sharetrace.util.logging.metric.CreateUsersRuntime;
import io.sharetrace.util.logging.metric.GraphSize;
import io.sharetrace.util.logging.metric.MsgPassingRuntime;
import io.sharetrace.util.logging.metric.RiskPropRuntime;
import io.sharetrace.util.logging.metric.SendContactsRuntime;
import io.sharetrace.util.logging.metric.SendScoresRuntime;
import io.sharetrace.util.logging.setting.ExperimentSettings;

public final class RuntimeExperiment extends Experiment<RuntimeExperimentConfig> {

  private static final int IGNORED = 50;
  private static final RuntimeExperiment INSTANCE = new RuntimeExperiment();
  private static final Context DEFAULT_CTX = newDefaultContext();

  private RuntimeExperiment() {}

  public static RuntimeExperiment instance() {
    return INSTANCE;
  }

  public static Context newDefaultContext() {
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
  public void run(RuntimeExperimentConfig config, State initialState) {
    for (int n : config.numNodes()) {
      Dataset dataset = ((SampledDataset) initialState.dataset()).withNumNodes(n);
      for (int iNetwork = 0; iNetwork < config.numNetworks(); iNetwork++) {
        initialState.toBuilder()
            .dataset(dataset.withNewContactNetwork())
            .build()
            .run(config.numIterations());
      }
    }
  }

  @Override
  public State newDefaultState(RuntimeExperimentConfig config) {
    return newDefaultState(DEFAULT_CTX, config);
  }

  @Override
  public State newDefaultState(Context ctx, RuntimeExperimentConfig config) {
    return State.builder(ctx)
        .graphType(getProperty(config.graphType(), "graphType"))
        .dataset(context -> Defaults.sampledDataset(context, IGNORED))
        .build();
  }
}

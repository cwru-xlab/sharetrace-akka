package io.sharetrace.experiment;

import io.sharetrace.experiment.config.RuntimeExperimentConfig;
import io.sharetrace.experiment.data.Dataset;
import io.sharetrace.experiment.data.SampledDataset;
import io.sharetrace.experiment.state.Context;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.State;
import io.sharetrace.util.logging.metric.CreateUsersRuntime;
import io.sharetrace.util.logging.metric.GraphSize;
import io.sharetrace.util.logging.metric.MessagePassingRuntime;
import io.sharetrace.util.logging.metric.RiskPropagationRuntime;
import io.sharetrace.util.logging.metric.SendContactsRuntime;
import io.sharetrace.util.logging.metric.SendRiskScoresRuntime;
import io.sharetrace.util.logging.setting.ExperimentSettings;

public final class RuntimeExperiment extends Experiment<RuntimeExperimentConfig> {

  private static final int IGNORED = 50;

  @Override
  public void run(RuntimeExperimentConfig config, State state) {
    for (int n : config.users()) {
      Dataset dataset = ((SampledDataset) state.dataset()).withUsers(n);
      for (int iNetwork = 0; iNetwork < config.networks(); iNetwork++) {
        state.toBuilder().dataset(dataset.withNewContactNetwork()).build().run(config.iterations());
      }
    }
  }

  @Override
  public Context defaultContext() {
    return Defaults.context()
        .withLoggable(
            GraphSize.class,
            CreateUsersRuntime.class,
            SendRiskScoresRuntime.class,
            SendContactsRuntime.class,
            RiskPropagationRuntime.class,
            MessagePassingRuntime.class,
            ExperimentSettings.class);
  }

  @Override
  public State newDefaultState(Context ctx, RuntimeExperimentConfig config) {
    return State.builder(ctx)
        .graphType(getProperty(config.graphType(), "graphType"))
        .dataset(context -> Defaults.sampledDataset(context, IGNORED))
        .build();
  }
}

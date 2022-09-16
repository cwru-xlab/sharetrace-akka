package org.sharetrace.experiment;

import java.util.Set;
import org.sharetrace.data.SampledDataset;
import org.sharetrace.experiment.config.RuntimeExperimentConfig;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentContext;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.logging.metric.CreateUsersRuntime;
import org.sharetrace.logging.metric.GraphSize;
import org.sharetrace.logging.metric.MsgPassingRuntime;
import org.sharetrace.logging.metric.RiskPropRuntime;
import org.sharetrace.logging.metric.SendContactsRuntime;
import org.sharetrace.logging.metric.SendScoresRuntime;
import org.sharetrace.logging.setting.ExperimentSettings;

public final class RuntimeExperiment implements Experiment<RuntimeExperimentConfig> {

  private static final RuntimeExperiment INSTANCE = new RuntimeExperiment();
  private static final int IGNORED = 50;
  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  private RuntimeExperiment() {}

  public static RuntimeExperiment instance() {
    return INSTANCE;
  }

  private static ExperimentContext newDefaultContext() {
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

  @Override
  public void run(ExperimentState initialState, RuntimeExperimentConfig config) {
    SampledDataset dataset = (SampledDataset) initialState.dataset();
    for (int n : config.numNodes()) {
      initialState.toBuilder().dataset(dataset.withNumNodes(n)).build().run();
    }
  }

  @Override
  public ExperimentState newDefaultState(RuntimeExperimentConfig config) {
    return ExperimentState.builder(DEFAULT_CTX)
        .graphType(getProperty(config.graphType(), "graphType"))
        .dataset(ctx -> Defaults.sampledDataset(ctx, IGNORED))
        .build();
  }
}

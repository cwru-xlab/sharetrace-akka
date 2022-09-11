package org.sharetrace.experiment;

import java.util.Set;
import org.immutables.value.Value;
import org.sharetrace.data.SampledDataset;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.logging.metric.CreateUsersRuntime;
import org.sharetrace.logging.metric.GraphSize;
import org.sharetrace.logging.metric.MsgPassingRuntime;
import org.sharetrace.logging.metric.RiskPropRuntime;
import org.sharetrace.logging.metric.SendContactsRuntime;
import org.sharetrace.logging.metric.SendScoresRuntime;
import org.sharetrace.logging.setting.ExperimentSettings;
import org.sharetrace.util.Range;

@Value.Immutable
abstract class BaseRuntimeExperiment implements Experiment {

  private static final int IGNORED = 50;
  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

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

  public static ExperimentState newDefaultState(GraphType graphType) {
    return ExperimentState.builder(DEFAULT_CTX)
        .graphType(graphType)
        .dataset(ctx -> Defaults.sampledDataset(ctx, IGNORED))
        .build();
  }

  public void runWithDefaults(GraphType graphType) {
    run(newDefaultState(graphType));
  }

  @Override
  public void run(ExperimentState initialState) {
    SampledDataset dataset = (SampledDataset) initialState.dataset();
    for (double n : numNodes()) {
      initialState.toBuilder().dataset(dataset.withNumNodes((int) n)).build().run();
    }
  }

  @Value.Parameter
  public abstract Range numNodes();
}

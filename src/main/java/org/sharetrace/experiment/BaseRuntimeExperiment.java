package org.sharetrace.experiment;

import java.util.Set;
import org.immutables.value.Value;
import org.sharetrace.data.SampledDataset;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.logging.metrics.GraphSize;
import org.sharetrace.logging.metrics.MsgPassingRuntime;
import org.sharetrace.logging.metrics.RiskPropRuntime;
import org.sharetrace.logging.metrics.SendContactsRuntime;
import org.sharetrace.logging.metrics.SendScoresRuntime;
import org.sharetrace.logging.settings.ExperimentSettings;
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
                SendScoresRuntime.class,
                SendContactsRuntime.class,
                RiskPropRuntime.class,
                MsgPassingRuntime.class,
                ExperimentSettings.class));
  }

  private static ExperimentState newDefaultState(GraphType graphType) {
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

package org.sharetrace.experiment;

import java.util.Set;
import org.immutables.value.Value;
import org.sharetrace.data.SampledDataset;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.experiment.state.SampledDatasetBuilder;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.logging.metrics.SizeMetrics;
import org.sharetrace.logging.settings.ExperimentSettings;
import org.sharetrace.util.Range;

@Value.Immutable
abstract class BaseRuntimeExperiment implements Experiment {

  private static final int IGNORED = 50;
  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  private static ExperimentContext newDefaultContext() {
    return ExperimentContext.create()
        .withLoggable(Set.of(SizeMetrics.class, RuntimeMetric.class, ExperimentSettings.class));
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
  protected abstract Range numNodes();

  private static ExperimentState newDefaultState(GraphType graphType) {
    return ExperimentState.builder(DEFAULT_CTX)
        .graphType(graphType)
        .dataset(ctx -> SampledDatasetBuilder.create().context(ctx).numNodes(IGNORED).build())
        .build();
  }
}

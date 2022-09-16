package org.sharetrace.experiment;

import java.util.Set;
import org.immutables.value.Value;
import org.sharetrace.data.SampledDataset;
import org.sharetrace.experiment.RuntimeExperiment.Inputs;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.logging.metric.CreateUsersRuntime;
import org.sharetrace.logging.metric.GraphSize;
import org.sharetrace.logging.metric.MsgPassingRuntime;
import org.sharetrace.logging.metric.RiskPropRuntime;
import org.sharetrace.logging.metric.SendContactsRuntime;
import org.sharetrace.logging.metric.SendScoresRuntime;
import org.sharetrace.logging.setting.ExperimentSettings;

@Value.Immutable
@Value.Enclosing
abstract class BaseRuntimeExperiment implements Experiment<Inputs> {

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

  @Override
  public void run(ExperimentState initialState) {
    SampledDataset dataset = (SampledDataset) initialState.dataset();
    for (int n : numNodes()) {
      initialState.toBuilder().dataset(dataset.withNumNodes(n)).build().run();
    }
  }

  @Override
  public ExperimentState newDefaultState(Inputs inputs) {
    return ExperimentState.builder(DEFAULT_CTX)
        .graphType(inputs.graphType())
        .dataset(ctx -> Defaults.sampledDataset(ctx, IGNORED))
        .build();
  }

  @Value.Parameter
  public abstract Iterable<Integer> numNodes();

  @Value.Immutable
  interface BaseInputs {

    @Value.Parameter
    GraphType graphType();
  }
}

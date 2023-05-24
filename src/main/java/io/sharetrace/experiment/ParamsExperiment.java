package io.sharetrace.experiment;

import io.sharetrace.experiment.config.ParamsExperimentConfig;
import io.sharetrace.experiment.data.Dataset;
import io.sharetrace.experiment.state.Context;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.State;
import io.sharetrace.graph.GraphType;
import io.sharetrace.util.Collecting;
import io.sharetrace.util.logging.Loggable;
import io.sharetrace.util.logging.metric.GraphTopology;
import java.util.Set;

public final class ParamsExperiment extends Experiment<ParamsExperimentConfig> {

  @Override
  public void run(ParamsExperimentConfig config, State state) {
    for (int i = 0; i < config.networks(); i++) {
      Dataset dataset = state.dataset();
      dataset = dataset.withNewContactNetwork().withScoreFactory(dataset.scoreFactory().cached());
      for (float transmissionRate : config.transmissionRates()) {
        for (float sendCoefficient : config.sendCoefficients()) {
          state.toBuilder()
              .userParameters(
                  state
                      .userParameters()
                      .withTransmissionRate(transmissionRate)
                      .withSendCoefficient(sendCoefficient))
              .dataset(dataset)
              .build()
              .run(config.iterations());
        }
      }
    }
  }

  @Override
  public Context defaultContext() {
    Context context = Defaults.context();
    Set<Class<? extends Loggable>> loggable = Collecting.newHashSet(context.loggable());
    loggable.remove(GraphTopology.class);
    return context.withLoggable(loggable);
  }

  @Override
  public State newDefaultState(Context context, ParamsExperimentConfig config) {
    GraphType graphType = getProperty(config.graphType(), "graphType");
    int numNodes = getProperty(config.users(), "users");
    return State.builder(context)
        .graphType(graphType)
        .dataset(ctx -> Defaults.sampledDataset(ctx, numNodes))
        .build();
  }
}

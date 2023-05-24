package io.sharetrace.experiment;

import io.sharetrace.experiment.config.NoiseExperimentConfig;
import io.sharetrace.experiment.data.Dataset;
import io.sharetrace.experiment.state.Context;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.State;
import io.sharetrace.util.logging.event.UpdateEvent;
import io.sharetrace.util.logging.metric.GraphSize;
import io.sharetrace.util.logging.setting.ExperimentSettings;
import org.apache.commons.math3.distribution.RealDistribution;

public final class NoiseExperiment extends Experiment<NoiseExperimentConfig> {

  @Override
  public void run(NoiseExperimentConfig config, State state) {
    for (int i = 0; i < config.networks(); i++) {
      Dataset dataset = state.dataset().withNewContactNetwork();
      for (RealDistribution noise : config.noises()) {
        state.toBuilder()
            .dataset(dataset.withScoreFactory(dataset.scoreFactory().cached().noisy(noise)))
            .build()
            .run(config.iterations());
      }
    }
  }

  @Override
  public Context defaultContext() {
    return Defaults.context()
        .withLoggable(UpdateEvent.class, GraphSize.class, ExperimentSettings.class);
  }

  @Override
  public State newDefaultState(Context context, NoiseExperimentConfig config) {
    return State.builder(context)
        .dataset(getProperty(config.datasetFactory(), "datasetFactory"))
        .graphType(getProperty(config.graphType(), "graphType"))
        .build();
  }
}

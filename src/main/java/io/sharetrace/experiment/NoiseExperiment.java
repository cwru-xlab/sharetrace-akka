package io.sharetrace.experiment;

import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.experiment.config.NoiseExperimentConfig;
import io.sharetrace.experiment.data.Dataset;
import io.sharetrace.experiment.data.factory.CachedRiskScoreFactory;
import io.sharetrace.experiment.data.factory.NoisyRiskScoreFactory;
import io.sharetrace.experiment.data.factory.RiskScoreFactory;
import io.sharetrace.experiment.state.Context;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.State;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.util.logging.event.UpdateEvent;
import io.sharetrace.util.logging.metric.GraphSize;
import io.sharetrace.util.logging.setting.ExperimentSettings;
import org.apache.commons.math3.distribution.RealDistribution;

public final class NoiseExperiment extends Experiment<NoiseExperimentConfig> {

  private static final Context DEFAULT_CTX = newDefaultContext();

  private static Context newDefaultContext() {
    return Defaults.context()
        .withLoggable(UpdateEvent.class, GraphSize.class, ExperimentSettings.class);
  }

  private static Dataset withNoisyScoreFactory(Dataset dataset, RealDistribution noise) {
    RiskScoreFactory cached = CachedRiskScoreFactory.of(dataset.scoreFactory());
    RiskScoreFactory noisy = NoisyRiskScoreFactory.of(cached, noise);
    return dataset.withScoreFactory(noisy);
  }

  /**
   * Evaluates the accuracy of {@link RiskPropagation} when noise is added to the user symptom
   * scores. For a given noise distribution, each {@link ContactNetwork} is 1 or more times. The
   * risk scores of the initial {@link Dataset} are used for all noise distributions to allow for
   * comparison. Each noise distribution is evaluated 1 or more times to allow for an average
   * accuracy to be measured.
   */
  @Override
  public void run(NoiseExperimentConfig config, State state) {
    for (int iNetwork = 0; iNetwork < config.numNetworks(); iNetwork++) {
      Dataset dataset = state.dataset().withNewContactNetwork();
      for (RealDistribution noise : config.noises()) {
        state.toBuilder()
            .dataset(withNoisyScoreFactory(dataset, noise))
            .build()
            .run(config.numIterations());
      }
    }
  }

  @Override
  public Context defaultContext() {
    return DEFAULT_CTX;
  }

  @Override
  public State newDefaultState(Context ctx, NoiseExperimentConfig config) {
    return State.builder(ctx)
        .dataset(getProperty(config.datasetFactory(), "datasetFactory"))
        .graphType(getProperty(config.graphType(), "graphType"))
        .build();
  }
}

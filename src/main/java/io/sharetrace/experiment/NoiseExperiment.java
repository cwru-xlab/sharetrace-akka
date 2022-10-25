package io.sharetrace.experiment;

import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.data.Dataset;
import io.sharetrace.data.factory.CachedRiskScoreFactory;
import io.sharetrace.data.factory.NoisyRiskScoreFactory;
import io.sharetrace.data.factory.RiskScoreFactory;
import io.sharetrace.experiment.config.NoiseExperimentConfig;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.ExperimentContext;
import io.sharetrace.experiment.state.ExperimentState;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.logging.event.UpdateEvent;
import io.sharetrace.logging.metric.GraphSize;
import io.sharetrace.logging.setting.ExperimentSettings;
import org.apache.commons.math3.distribution.RealDistribution;

public final class NoiseExperiment extends Experiment<NoiseExperimentConfig> {

  private static final String DATASET_FACTORY = "datasetFactory";
  private static final String GRAPH_TYPE = "graphType";

  private static final NoiseExperiment INSTANCE = new NoiseExperiment();
  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  private NoiseExperiment() {}

  public static NoiseExperiment instance() {
    return INSTANCE;
  }

  public static ExperimentContext newDefaultContext() {
    return Defaults.context()
        .withLoggable(UpdateEvent.class, GraphSize.class, ExperimentSettings.class);
  }

  /**
   * Evaluates the accuracy of {@link RiskPropagation} when noise is added to the user symptom
   * scores. For a given noise distribution, each {@link ContactNetwork} is 1 or more times. The
   * risk scores of the initial {@link Dataset} are used for all noise distributions to allow for
   * comparison. Each noise distribution is evaluated 1 or more times to allow for an average
   * accuracy to be measured.
   */
  @Override
  public void run(NoiseExperimentConfig config, ExperimentState initialState) {
    for (int iNetwork = 0; iNetwork < config.numNetworks(); iNetwork++) {
      Dataset dataset = initialState.dataset().withNewContactNetwork();
      for (RealDistribution noise : config.noises()) {
        initialState.toBuilder()
            .dataset(withNoisyScoreFactory(dataset, noise))
            .build()
            .run(config.numIterations());
      }
    }
  }

  private static Dataset withNoisyScoreFactory(Dataset dataset, RealDistribution noise) {
    RiskScoreFactory cached = CachedRiskScoreFactory.of(dataset.scoreFactory());
    RiskScoreFactory noisy = NoisyRiskScoreFactory.of(cached, noise);
    return dataset.withScoreFactory(noisy);
  }

  @Override
  public ExperimentState newDefaultState(NoiseExperimentConfig config) {
    return newDefaultState(DEFAULT_CTX, config);
  }

  @Override
  public ExperimentState newDefaultState(ExperimentContext context, NoiseExperimentConfig config) {
    return ExperimentState.builder(context)
        .dataset(getProperty(config.datasetFactory(), DATASET_FACTORY))
        .graphType(getProperty(config.graphType(), GRAPH_TYPE))
        .build();
  }
}

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

  private static RiskScoreFactory newNoisyFactory(
      RiskScoreFactory factory, RealDistribution noise) {
    return NoisyRiskScoreFactory.of(noise, CachedRiskScoreFactory.of(factory));
  }

  /**
   * Evaluates the accuracy of {@link RiskPropagation} when noise is added to the user symptom
   * scores. For a given noise distribution, each {@link ContactNetwork} is 1 or more times. The
   * risk scores of the initial {@link Dataset} are used for all noise distributions to allow for
   * comparison. Each noise distribution is evaluated 1 or more times to allow for an average
   * accuracy to be measured.
   */
  @Override
  public void run(ExperimentState initialState, NoiseExperimentConfig config) {
    Dataset dataset = initialState.dataset();
    RiskScoreFactory noisyFactory;
    for (int iNetwork = 0; iNetwork < config.numNetworks(); iNetwork++) {
      dataset = dataset.withNewContactNetwork();
      for (RealDistribution noise : config.noises()) {
        noisyFactory = newNoisyFactory(dataset, noise);
        initialState.toBuilder()
            .dataset(dataset.withScoreFactory(noisyFactory))
            .userParams(ctx -> Defaults.userParams(ctx.dataset()))
            .build()
            .run(config.numIterations());
      }
    }
  }

  @Override
  public ExperimentState newDefaultState(NoiseExperimentConfig config) {
    return newDefaultState(DEFAULT_CTX, config);
  }

  @Override
  public ExperimentState newDefaultState(ExperimentContext context, NoiseExperimentConfig config) {
    return ExperimentState.builder(context)
        .dataset(getProperty(config.datasetFactory(), "datasetFactory"))
        .graphType(getProperty(config.graphType(), "graphType"))
        .build();
  }
}

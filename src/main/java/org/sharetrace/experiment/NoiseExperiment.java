package org.sharetrace.experiment;

import java.util.Set;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.sharetrace.data.factory.CachedRiskScoreFactory;
import org.sharetrace.data.factory.NoisyRiskScoreFactory;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.experiment.config.NoiseExperimentConfig;
import org.sharetrace.experiment.state.ExperimentContext;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.logging.event.UpdateEvent;
import org.sharetrace.logging.metric.GraphSize;
import org.sharetrace.logging.setting.ExperimentSettings;

public final class NoiseExperiment implements Experiment<NoiseExperimentConfig> {

  private static final NoiseExperiment INSTANCE = new NoiseExperiment();
  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();
  private static final RealDistribution IGNORED = new UniformRealDistribution();

  private NoiseExperiment() {}

  public static NoiseExperiment instance() {
    return INSTANCE;
  }

  private static ExperimentState withScoreFactory(ExperimentState state, RiskScoreFactory factory) {
    return state.toBuilder().dataset(state.dataset().withScoreFactory(factory)).build();
  }

  private static NoisyRiskScoreFactory newNoisyScoreFactory(ExperimentState initialState) {
    return NoisyRiskScoreFactory.of(IGNORED, CachedRiskScoreFactory.of(initialState.dataset()));
  }

  private static ExperimentContext newDefaultContext() {
    return ExperimentContext.create()
        .withLoggable(Set.of(UpdateEvent.class, GraphSize.class, ExperimentSettings.class));
  }

  @Override
  public void run(ExperimentState initialState, NoiseExperimentConfig config) {
    NoisyRiskScoreFactory noisyFactory = newNoisyScoreFactory(initialState);
    for (RealDistribution noise : config.noises()) {
      ExperimentState state = withScoreFactory(initialState, noisyFactory.withNoise(noise));
      config.numIterations().forEach(x -> state.withNewId().run());
    }
  }

  @Override
  public ExperimentState newDefaultState(NoiseExperimentConfig config) {
    return ExperimentState.builder(DEFAULT_CTX)
        .dataset(getProperty(config.datasetFactory(), "datasetFactory"))
        .graphType(getProperty(config.graphType(), "graphType"))
        .build();
  }
}

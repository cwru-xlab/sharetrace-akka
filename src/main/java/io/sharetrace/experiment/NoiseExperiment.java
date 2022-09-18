package io.sharetrace.experiment;

import io.sharetrace.data.factory.CachedRiskScoreFactory;
import io.sharetrace.data.factory.NoisyRiskScoreFactory;
import io.sharetrace.data.factory.RiskScoreFactory;
import io.sharetrace.experiment.config.NoiseExperimentConfig;
import io.sharetrace.experiment.state.ExperimentContext;
import io.sharetrace.experiment.state.ExperimentState;
import io.sharetrace.logging.event.UpdateEvent;
import io.sharetrace.logging.metric.GraphSize;
import io.sharetrace.logging.setting.ExperimentSettings;
import java.util.Set;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;

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

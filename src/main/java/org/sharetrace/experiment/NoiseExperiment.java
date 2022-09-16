package org.sharetrace.experiment;

import java.util.Set;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.FileDataset;
import org.sharetrace.data.SampledDataset;
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

  private static RiskScoreFactory cachedScoreFactory(ExperimentState state) {
    Dataset dataset = state.dataset();
    return CachedRiskScoreFactory.of(
        dataset instanceof FileDataset
            ? ((FileDataset) dataset).scoreFactory()
            : ((SampledDataset) dataset).scoreFactory());
  }

  private static ExperimentState withScoreFactory(ExperimentState state, RiskScoreFactory factory) {
    Dataset dataset = state.dataset();
    return state.toBuilder()
        .dataset(
            dataset instanceof FileDataset
                ? ((FileDataset) dataset).withScoreFactory(factory)
                : ((SampledDataset) dataset).withScoreFactory(factory))
        .build();
  }

  private static NoisyRiskScoreFactory newNoisyScoreFactory(ExperimentState initialState) {
    return NoisyRiskScoreFactory.of(IGNORED, cachedScoreFactory(initialState));
  }

  private static ExperimentContext newDefaultContext() {
    return ExperimentContext.create()
        .withLoggable(Set.of(UpdateEvent.class, GraphSize.class, ExperimentSettings.class));
  }

  @Override
  public void run(ExperimentState initialState, NoiseExperimentConfig config) {
    NoisyRiskScoreFactory noisy = newNoisyScoreFactory(initialState);
    for (RealDistribution noise : config.noises()) {
      ExperimentState state = withScoreFactory(initialState, noisy.withNoise(noise));
      config.numIterations().forEach(x -> state.withNewId().run());
    }
  }

  @Override
  public ExperimentState newDefaultState(NoiseExperimentConfig config) {
    GraphType graphType = getProperty(config.graphType(), "graphType");
    Dataset dataset = getProperty(config.dataset(), "dataset");
    return ExperimentState.builder(DEFAULT_CTX).dataset(dataset).graphType(graphType).build();
  }
}

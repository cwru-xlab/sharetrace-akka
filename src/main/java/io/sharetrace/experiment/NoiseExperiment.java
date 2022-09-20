package io.sharetrace.experiment;

import io.sharetrace.data.Dataset;
import io.sharetrace.data.factory.CachedRiskScoreFactory;
import io.sharetrace.data.factory.NoisyRiskScoreFactory;
import io.sharetrace.data.factory.RiskScoreFactory;
import io.sharetrace.experiment.config.NoiseExperimentConfig;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.ExperimentContext;
import io.sharetrace.experiment.state.ExperimentState;
import io.sharetrace.logging.event.UpdateEvent;
import io.sharetrace.logging.metric.GraphSize;
import io.sharetrace.logging.setting.ExperimentSettings;
import io.sharetrace.util.range.IntRange;
import java.util.Set;
import org.apache.commons.math3.distribution.RealDistribution;

public final class NoiseExperiment implements Experiment<NoiseExperimentConfig> {

  private static final NoiseExperiment INSTANCE = new NoiseExperiment();
  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  private NoiseExperiment() {}

  public static NoiseExperiment instance() {
    return INSTANCE;
  }

  private static RiskScoreFactory newNoisyFactory(
      RiskScoreFactory factory, RealDistribution noise) {
    // Cache the original scores so that the independent effect of noise can be observed.
    return NoisyRiskScoreFactory.of(noise, CachedRiskScoreFactory.of(factory));
  }

  public static ExperimentContext newDefaultContext() {
    return ExperimentContext.create()
        .withLoggable(Set.of(UpdateEvent.class, GraphSize.class, ExperimentSettings.class));
  }

  private static ExperimentState withNewNetworkAndFactory(
      ExperimentState state, Dataset withNewNetwork, RealDistribution noise) {
    RiskScoreFactory noisyFactory = newNoisyFactory(withNewNetwork, noise);
    return state.toBuilder()
        .dataset(withNewNetwork.withScoreFactory(noisyFactory))
        .userParams(ctx -> Defaults.userParams(ctx.dataset()))
        .build();
  }

  private static void forEachNetwork(ExperimentState state, NoiseExperimentConfig config) {
    Dataset withNewNetwork = state.dataset().withNewContactNetwork();
    for (RealDistribution noise : config.noises()) {
      ExperimentState newState = withNewNetworkAndFactory(state, withNewNetwork, noise);
      IntRange.of(config.numIterations()).forEach(x -> newState.withNewId().run());
    }
  }

  @Override
  public void run(ExperimentState initialState, NoiseExperimentConfig config) {
    IntRange.of(config.numIterations()).forEach(x -> forEachNetwork(initialState, config));
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

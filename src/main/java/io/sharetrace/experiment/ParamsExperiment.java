package io.sharetrace.experiment;

import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.data.Dataset;
import io.sharetrace.data.factory.CachedRiskScoreFactory;
import io.sharetrace.experiment.config.ParamsExperimentConfig;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.ExperimentContext;
import io.sharetrace.experiment.state.ExperimentState;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.logging.Loggable;
import io.sharetrace.logging.metric.GraphTopology;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;

public final class ParamsExperiment extends Experiment<ParamsExperimentConfig> {

  private static final ParamsExperiment INSTANCE = new ParamsExperiment();
  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  private ParamsExperiment() {}

  public static ParamsExperiment instance() {
    return INSTANCE;
  }

  public static ExperimentContext newDefaultContext() {
    ExperimentContext ctx = Defaults.context();
    Set<Class<? extends Loggable>> loggable = new ObjectOpenHashSet<>(ctx.loggable());
    loggable.remove(GraphTopology.class);
    return ctx.withLoggable(loggable);
  }

  /**
   * Evaluates the effects of the transmission rate and the send coefficient on the accuracy and
   * efficiency of {@link RiskPropagation}. Risk scores are cached across parameter values for a
   * given {@link ContactNetwork} so that cross-parameter comparisons are possible.
   */
  @Override
  public void run(ParamsExperimentConfig config, ExperimentState initialState) {
    for (int iNetwork = 0; iNetwork < config.numIterations(); iNetwork++) {
      Dataset dataset = cacheScores(initialState.dataset().withNewContactNetwork());
      for (float tr : config.transRates()) {
        for (float sc : config.sendCoeffs()) {
          initialState.toBuilder()
              .userParams(initialState.userParams().withTransRate(tr).withSendCoeff(sc))
              .dataset(dataset)
              .build()
              .run();
        }
      }
    }
  }

  private static Dataset cacheScores(Dataset dataset) {
    return dataset.withScoreFactory(CachedRiskScoreFactory.of(dataset));
  }

  @Override
  public ExperimentState newDefaultState(ParamsExperimentConfig config) {
    return newDefaultState(DEFAULT_CTX, config);
  }

  @Override
  public ExperimentState newDefaultState(ExperimentContext context, ParamsExperimentConfig config) {
    GraphType graphType = getProperty(config.graphType(), "graphType");
    int numNodes = getProperty(config.numNodes(), "numNodes");
    return ExperimentState.builder(context)
        .graphType(graphType)
        .dataset(ctx -> Defaults.sampledDataset(ctx, numNodes))
        .build();
  }
}

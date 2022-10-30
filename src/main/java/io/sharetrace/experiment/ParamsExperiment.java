package io.sharetrace.experiment;

import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.data.factory.CachedRiskScoreFactory;
import io.sharetrace.experiment.config.ParamsExperimentConfig;
import io.sharetrace.experiment.data.Dataset;
import io.sharetrace.experiment.data.factory.RiskScoreFactory;
import io.sharetrace.experiment.state.Context;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.State;
import io.sharetrace.graph.ContactNetwork;
import io.sharetrace.logging.metric.GraphTopology;
import io.sharetrace.util.logging.Loggable;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Set;

public final class ParamsExperiment extends Experiment<ParamsExperimentConfig> {

  private static final ParamsExperiment INSTANCE = new ParamsExperiment();
  private static final Context DEFAULT_CTX = newDefaultContext();

  private ParamsExperiment() {}

  public static ParamsExperiment instance() {
    return INSTANCE;
  }

  public static Context newDefaultContext() {
    Context ctx = Defaults.context();
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
  public void run(ParamsExperimentConfig config, State initialState) {
    for (int iNetwork = 0; iNetwork < config.numNetworks(); iNetwork++) {
      Dataset dataset = cacheScores(initialState.dataset().withNewContactNetwork());
      for (float tr : config.transRates()) {
        for (float sc : config.sendCoeffs()) {
          initialState.toBuilder()
              .userParams(initialState.userParams().withTransRate(tr).withSendCoeff(sc))
              .dataset(dataset)
              .build()
              .run(config.numIterations());
        }
      }
    }
  }

  private static Dataset cacheScores(Dataset dataset) {
    RiskScoreFactory cachedScoreFactory = CachedRiskScoreFactory.of(dataset.scoreFactory());
    return dataset.withScoreFactory(cachedScoreFactory);
  }

  @Override
  public State newDefaultState(ParamsExperimentConfig config) {
    return newDefaultState(DEFAULT_CTX, config);
  }

  @Override
  public State newDefaultState(Context ctx, ParamsExperimentConfig config) {
    GraphType graphType = getProperty(config.graphType(), "graphType");
    int numNodes = getProperty(config.numNodes(), "numNodes");
    return State.builder(ctx)
        .graphType(graphType)
        .dataset(context -> Defaults.sampledDataset(context, numNodes))
        .build();
  }
}

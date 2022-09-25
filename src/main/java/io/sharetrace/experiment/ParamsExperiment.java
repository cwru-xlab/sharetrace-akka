package io.sharetrace.experiment;

import io.sharetrace.experiment.config.ParamsExperimentConfig;
import io.sharetrace.experiment.state.Defaults;
import io.sharetrace.experiment.state.ExperimentContext;
import io.sharetrace.experiment.state.ExperimentState;
import io.sharetrace.logging.Loggable;
import io.sharetrace.logging.metric.GraphTopology;
import io.sharetrace.util.range.IntRange;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collections;
import java.util.Set;

public final class ParamsExperiment extends Experiment<ParamsExperimentConfig> {

  private static final ParamsExperiment INSTANCE = new ParamsExperiment();
  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  private ParamsExperiment() {}

  public static ParamsExperiment instance() {
    return INSTANCE;
  }

  public static ExperimentContext newDefaultContext() {
    ExperimentContext ctx = ExperimentContext.create();
    Set<Class<? extends Loggable>> loggable = new ObjectOpenHashSet<>(ctx.loggable());
    loggable.remove(GraphTopology.class);
    return ctx.withLoggable(Collections.unmodifiableSet(loggable));
  }

  private static void forEachParameters(ExperimentState state, float transRate, float sendCoeff) {
    state.toBuilder()
        .msgParams(state.msgParams().withTransRate(transRate).withSendCoeff(sendCoeff))
        .dataset(state.dataset().withNewContactNetwork())
        .userParams(ctx -> Defaults.userParams(ctx.dataset()))
        .build()
        .run();
  }

  @Override
  public void run(ExperimentState initialState, ParamsExperimentConfig config) {
    for (float tr : config.transRates()) {
      for (float sc : config.sendCoeffs()) {
        IntRange.of(config.numIterations()).forEach(x -> forEachParameters(initialState, tr, sc));
      }
    }
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

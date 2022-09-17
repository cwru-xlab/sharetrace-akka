package org.sharetrace.experiment;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collections;
import java.util.Set;
import org.sharetrace.experiment.config.ParamsExperimentConfig;
import org.sharetrace.experiment.state.Defaults;
import org.sharetrace.experiment.state.ExperimentContext;
import org.sharetrace.experiment.state.ExperimentState;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.metric.GraphTopology;
import org.sharetrace.model.MsgParams;

public final class ParamsExperiment implements Experiment<ParamsExperimentConfig> {

  private static final ParamsExperiment INSTANCE = new ParamsExperiment();
  private static final ExperimentContext DEFAULT_CTX = newDefaultContext();

  private ParamsExperiment() {}

  public static ParamsExperiment instance() {
    return INSTANCE;
  }

  private static ExperimentContext newDefaultContext() {
    ExperimentContext ctx = ExperimentContext.create();
    Set<Class<? extends Loggable>> loggable = new ObjectOpenHashSet<>(ctx.loggable());
    loggable.remove(GraphTopology.class);
    return ctx.withLoggable(Collections.unmodifiableSet(loggable));
  }

  @Override
  public void run(ExperimentState initialState, ParamsExperimentConfig config) {
    MsgParams msgParams;
    for (float tr : config.transRates()) {
      for (float sc : config.sendCoeffs()) {
        msgParams = initialState.msgParams().withTransRate(tr).withSendCoeff(sc);
        initialState.toBuilder().msgParams(msgParams).build().run();
      }
    }
  }

  @Override
  public ExperimentState newDefaultState(ParamsExperimentConfig config) {
    GraphType graphType = getProperty(config.graphType(), "graphType");
    int numNodes = getProperty(config.numNodes(), "numNodes");
    return ExperimentState.builder(DEFAULT_CTX)
        .graphType(graphType)
        .dataset(ctx -> Defaults.sampledDataset(ctx, numNodes))
        .build();
  }
}

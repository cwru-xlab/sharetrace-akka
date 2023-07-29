package sharetrace.algorithm;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.slf4j.MDC;
import sharetrace.Buildable;
import sharetrace.graph.ContactNetwork;
import sharetrace.graph.TemporalNetworkExporter;
import sharetrace.logging.metric.GraphCycles;
import sharetrace.logging.metric.GraphEccentricity;
import sharetrace.logging.metric.GraphScores;
import sharetrace.logging.metric.GraphSize;
import sharetrace.logging.metric.GraphTopology;
import sharetrace.logging.metric.MetricRecord;
import sharetrace.logging.setting.ExperimentSettings;
import sharetrace.logging.setting.ExperimentSettingsBuilder;
import sharetrace.model.Parameters;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.model.message.Run;
import sharetrace.util.Context;
import sharetrace.util.ContextBuilder;
import sharetrace.util.IdFactory;

@Buildable
public record RiskPropagation<V>(
    Context context,
    Parameters parameters,
    RiskScoreFactory riskScoreFactory,
    ContactNetwork<V> contactNetwork)
    implements Runnable {

  public void run(int iterations) {
    IntStream.range(0, iterations).forEach(x -> run());
  }

  @Override
  public void run() {
    var context = contextWithMdc();
    MDC.setContextMap(context.mdc());
    logSettings(context);
    logMetrics(context);
    invoke(context);
  }

  private Context contextWithMdc() {
    var mdc = new HashMap<>(context.mdc());
    mdc.put("runId", IdFactory.nextUlid());
    return ContextBuilder.from(context).withMdc(mdc);
  }

  private void logSettings(Context context) {
    context.settingsLogger().log(ExperimentSettings.class, () -> settings(context));
  }

  private ExperimentSettings settings(Context context) {
    return ExperimentSettingsBuilder.create()
        .context(context)
        .parameters(parameters)
        .networkId(contactNetwork.id())
        .build();
  }

  private void logMetrics(Context context) {
    var logger = context.metricsLogger();
    logger.log(GraphSize.class, metric(GraphSize::of));
    logger.log(GraphCycles.class, metric(GraphCycles::of));
    logger.log(GraphEccentricity.class, metric(GraphEccentricity::of));
    logger.log(GraphScores.class, metric(GraphScores::of));
    if (logger.log(GraphTopology.class, metric(GraphTopology::of))) {
      TemporalNetworkExporter.export(contactNetwork, context.logDirectory(), contactNetwork.id());
    }
  }

  private <T extends MetricRecord> Supplier<T> metric(Function<ContactNetwork<V>, T> factory) {
    return () -> factory.apply(contactNetwork);
  }

  private void invoke(Context context) {
    try {
      newInstance(context).getWhenTerminated().toCompletableFuture().get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    } catch (ExecutionException exception) {
      throw new RuntimeException(exception);
    }
  }

  private ActorSystem<Void> newInstance(Context context) {
    return ActorSystem.create(behavior(context), RiskPropagation.class.getSimpleName());
  }

  private Behavior<Void> behavior(Context context) {
    return Behaviors.setup(
        ctx -> {
          var monitor = Monitor.of(context, parameters, riskScoreFactory, contactNetwork);
          var reference = ctx.spawn(monitor, Monitor.name(), Monitor.props());
          ctx.watch(reference);
          reference.tell(Run.INSTANCE);
          return Behaviors.receive(Void.class)
              .onSignal(Terminated.class, x -> Behaviors.stopped())
              .build();
        });
  }
}

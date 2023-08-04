package sharetrace.algorithm;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import org.slf4j.MDC;
import sharetrace.Buildable;
import sharetrace.graph.ContactNetwork;
import sharetrace.logging.LogRecord;
import sharetrace.logging.setting.ExperimentSettings;
import sharetrace.logging.setting.ExperimentSettingsBuilder;
import sharetrace.model.Parameters;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.model.message.Run;
import sharetrace.util.Context;
import sharetrace.util.ContextBuilder;
import sharetrace.util.IdFactory;

@Buildable
public record RiskPropagation(
    Context context,
    Parameters parameters,
    RiskScoreFactory riskScoreFactory,
    ContactNetwork contactNetwork)
    implements Runnable {

  public void run(int iterations) {
    IntStream.range(0, iterations).forEach(x -> run());
  }

  @Override
  public void run() {
    var context = contextWithMdc();
    MDC.setContextMap(context.mdc());
    logSettings(context);
    invoke(context);
  }

  private Context contextWithMdc() {
    var mdc = new Object2ObjectOpenHashMap<>(context.mdc());
    mdc.put(LogRecord.key(), IdFactory.nextUlid());
    return ContextBuilder.from(context).withMdc(mdc);
  }

  private void logSettings(Context context) {
    context.settingsLogger().log(ExperimentSettings.class, () -> settings(context));
  }

  private ExperimentSettings settings(Context context) {
    return ExperimentSettingsBuilder.create()
        .context(context)
        .parameters(parameters)
        .contactNetwork(contactNetwork)
        .build();
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

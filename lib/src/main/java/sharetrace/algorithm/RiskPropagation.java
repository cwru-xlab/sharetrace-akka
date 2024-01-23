package sharetrace.algorithm;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import org.slf4j.MDC;
import sharetrace.Buildable;
import sharetrace.graph.ContactNetwork;
import sharetrace.logging.ExecutionProperties;
import sharetrace.logging.ExecutionPropertiesBuilder;
import sharetrace.model.Parameters;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.model.message.RunMessage;
import sharetrace.util.Context;
import sharetrace.util.ContextBuilder;
import sharetrace.util.KeyFactory;

@Buildable
public record RiskPropagation(
    Context context,
    Parameters parameters,
    RiskScoreFactory riskScoreFactory,
    ContactNetwork contactNetwork,
    KeyFactory keyFactory)
    implements Runnable {

  public void run(int n) {
    IntStream.range(0, n).forEach(x -> run());
  }

  @Override
  public void run() {
    var context = contextWithMdc();
    MDC.setContextMap(context.mdc());
    logProperties(context);
    run(context);
  }

  private Context contextWithMdc() {
    return ContextBuilder.builder(context).addMdc("key", keyFactory.getKey()).build();
  }

  private void logProperties(Context context) {
    context.propertiesLogger().log(ExecutionProperties.class, () -> properties(context));
  }

  private ExecutionProperties properties(Context context) {
    return ExecutionPropertiesBuilder.create()
        .context(context)
        .parameters(parameters)
        .contactNetwork(contactNetwork)
        .build();
  }

  private void run(Context context) {
    try {
      ActorSystem.create(behavior(context), getClass().getSimpleName())
          .getWhenTerminated()
          .toCompletableFuture()
          .get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    } catch (ExecutionException exception) {
      throw new RuntimeException(exception);
    }
  }

  private Behavior<Void> behavior(Context context) {
    return Behaviors.setup(
        ctx -> {
          var monitor = Monitor.of(context, parameters, riskScoreFactory, contactNetwork);
          var ref = ctx.spawn(monitor, Monitor.name(), Monitor.props());
          ctx.watch(ref);
          ref.tell(RunMessage.INSTANCE);
          return Behaviors.receive(Void.class)
              .onSignal(Terminated.class, x -> Behaviors.stopped())
              .build();
        });
  }
}

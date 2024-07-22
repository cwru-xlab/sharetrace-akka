package sharetrace.algorithm;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;
import org.slf4j.MDC;
import sharetrace.Buildable;
import sharetrace.logging.ExecutionProperties;
import sharetrace.logging.ExecutionPropertiesBuilder;
import sharetrace.model.Context;
import sharetrace.model.ContextBuilder;
import sharetrace.model.Parameters;
import sharetrace.model.factory.ContactNetworkFactory;
import sharetrace.model.factory.KeyFactory;
import sharetrace.model.factory.RiskScoreFactory;
import sharetrace.model.message.RunMessage;

@Buildable
public record RiskPropagation(
    Context context,
    Parameters parameters,
    RiskScoreFactory scoreFactory,
    ContactNetworkFactory networkFactory,
    KeyFactory keyFactory)
    implements Runnable {

  public void run(int n) {
    IntStream.range(0, n).forEach(x -> run());
  }

  @Override
  public void run() {
    var properties = getExecutionProperties();
    logProperties(properties);
    run(properties);
  }

  private ExecutionProperties getExecutionProperties() {
    return ExecutionPropertiesBuilder.create()
        .context(ContextBuilder.builder(context).addMdc("k", keyFactory.getKey()).build())
        .parameters(parameters)
        .scoreFactory(scoreFactory)
        .network(networkFactory.getContactNetwork())
        .networkFactory(networkFactory)
        .keyFactory(keyFactory)
        .build();
  }

  private void logProperties(ExecutionProperties properties) {
    MDC.setContextMap(properties.context().mdc());
    context.propertyLogger().log(properties);
  }

  private void run(ExecutionProperties properties) {
    try {
      ActorSystem.create(behavior(properties), "RiskPropagation")
          .getWhenTerminated()
          .toCompletableFuture()
          .get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private Behavior<Void> behavior(ExecutionProperties p) {
    return Behaviors.setup(
        context -> {
          var behavior = Monitor.of(p.context(), p.parameters(), p.scoreFactory(), p.network());
          var props = DispatcherSelector.fromConfig("sharetrace.monitor.dispatcher");
          var ref = context.spawn(behavior, "Monitor", props);
          context.watch(ref);
          ref.tell(RunMessage.INSTANCE);
          return Behaviors.receive(Void.class)
              .onSignal(Terminated.class, x -> Behaviors.stopped())
              .build();
        });
  }
}

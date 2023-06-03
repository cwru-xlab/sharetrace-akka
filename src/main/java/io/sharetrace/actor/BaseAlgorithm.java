package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Props;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import io.sharetrace.model.message.AlgorithmMessage;
import io.sharetrace.model.message.RunMessage;
import java.util.concurrent.ExecutionException;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseAlgorithm implements Runnable {

  private static void waitUntilDone(ActorSystem<Void> running) {
    try {
      running.getWhenTerminated().toCompletableFuture().get();
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    } catch (ExecutionException exception) {
      throw new RuntimeException(exception);
    }
  }

  @Override
  public void run() {
    waitUntilDone(ActorSystem.create(newInstance(), name()));
  }

  protected abstract Behavior<AlgorithmMessage> behavior();

  protected abstract String name();

  protected abstract Props properties();

  private Behavior<Void> newInstance() {
    return Behaviors.setup(
        context -> {
          ActorRef<AlgorithmMessage> instance = context.spawn(behavior(), name(), properties());
          context.watch(instance);
          instance.tell(RunMessage.INSTANCE);
          return Behaviors.receive(Void.class)
              .onSignal(Terminated.class, x -> Behaviors.stopped())
              .build();
        });
  }
}

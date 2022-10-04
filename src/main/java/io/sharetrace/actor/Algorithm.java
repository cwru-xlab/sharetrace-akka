package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.Props;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import io.sharetrace.message.AlgorithmMsg;
import io.sharetrace.message.RunMsg;
import java.util.concurrent.ExecutionException;

public final class Algorithm implements Runnable {

  private final Props props;
  private final Behavior<AlgorithmMsg> behavior;
  private final String name;

  private Algorithm(Behavior<AlgorithmMsg> behavior, String name) {
    this.props = DispatcherSelector.fromConfig("algorithm-dispatcher");
    this.behavior = behavior;
    this.name = name;
  }

  public static Runnable of(Behavior<AlgorithmMsg> behavior, String name) {
    return new Algorithm(behavior, name);
  }

  @Override
  public void run() {
    waitUntilDone(ActorSystem.create(Behaviors.setup(this::newInstance), name));
  }

  private static void waitUntilDone(ActorSystem<Void> running) {
    try {
      running.getWhenTerminated().toCompletableFuture().get();
    } catch (InterruptedException | ExecutionException exception) {
      throw new RuntimeException(exception);
    }
  }

  private Behavior<Void> newInstance(ActorContext<Void> ctx) {
    ActorRef<AlgorithmMsg> instance = ctx.spawn(behavior, name, props);
    ctx.watch(instance);
    instance.tell(RunMsg.INSTANCE);
    return Behaviors.receive(Void.class)
        .onSignal(Terminated.class, x -> Behaviors.stopped())
        .build();
  }
}

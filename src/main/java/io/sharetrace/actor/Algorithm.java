package io.sharetrace.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import io.sharetrace.message.AlgorithmMsg;
import io.sharetrace.message.RunMsg;

public final class Algorithm {

  private final Behavior<AlgorithmMsg> behavior;
  private final String name;

  private Algorithm(Behavior<AlgorithmMsg> behavior, String name) {
    this.behavior = behavior;
    this.name = name;
  }

  public static Algorithm of(Behavior<AlgorithmMsg> behavior, String name) {
    return new Algorithm(behavior, name);
  }

  public void run() {
    ActorSystem.create(Behaviors.setup(this::newRunner), name);
  }

  private Behavior<Void> newRunner(ActorContext<Void> ctx) {
    ActorRef<AlgorithmMsg> instance = ctx.spawn(behavior, name);
    ctx.watch(instance);
    instance.tell(RunMsg.INSTANCE);
    return Behaviors.receive(Void.class)
        .onSignal(Terminated.class, x -> Behaviors.stopped())
        .build();
  }
}

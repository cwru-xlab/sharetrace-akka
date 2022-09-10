package org.sharetrace.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import org.sharetrace.message.AlgorithmMsg;
import org.sharetrace.message.RunMsg;

public class Runner {

  private Runner() {}

  public static void run(Behavior<AlgorithmMsg> algorithm, String name) {
    Behavior<Void> runner = Behaviors.setup(ctx -> newRunner(ctx, algorithm, name));
    ActorSystem.create(runner, name + "Runner");
  }

  private static Behavior<Void> newRunner(
      ActorContext<Void> context, Behavior<AlgorithmMsg> algorithm, String name) {
    ActorRef<AlgorithmMsg> instance = context.spawn(algorithm, name);
    context.watch(instance);
    instance.tell(RunMsg.INSTANCE);
    return Behaviors.receive(Void.class)
        .onSignal(Terminated.class, x -> Behaviors.stopped())
        .build();
  }
}

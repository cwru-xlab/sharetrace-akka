package org.sharetrace;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import org.sharetrace.message.AlgorithmMsg;
import org.sharetrace.message.RunMsg;

public class Runner {

  private Runner() {}

  public static void run(Behavior<AlgorithmMsg> algorithm, String name) {
    ActorSystem.create(runner(algorithm, name), name + "Runner");
  }

  private static Behavior<Void> runner(Behavior<AlgorithmMsg> algorithm, String name) {
    return Behaviors.setup(
        ctx -> {
          ActorRef<AlgorithmMsg> instance = ctx.spawn(algorithm, name);
          ctx.watch(instance);
          instance.tell(RunMsg.INSTANCE);
          return Behaviors.receive(Void.class)
              .onSignal(Terminated.class, x -> Behaviors.stopped())
              .build();
        });
  }
}

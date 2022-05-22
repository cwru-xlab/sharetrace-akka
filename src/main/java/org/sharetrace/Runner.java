package org.sharetrace;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import org.sharetrace.message.AlgorithmMessage;
import org.sharetrace.message.RunMessage;

public class Runner {

  private Runner() {}

  public static void run(Behavior<AlgorithmMessage> algorithm, String name) {
    ActorSystem.create(runner(algorithm, name), name + "Runner");
  }

  private static Behavior<Void> runner(Behavior<AlgorithmMessage> algorithm, String name) {
    return Behaviors.setup(
        context -> {
          ActorRef<AlgorithmMessage> instance = context.spawn(algorithm, name);
          context.watch(instance);
          instance.tell(RunMessage.INSTANCE);
          return Behaviors.receive(Void.class)
              .onSignal(Terminated.class, signal -> Behaviors.stopped())
              .build();
        });
  }
}

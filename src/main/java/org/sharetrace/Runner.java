package org.sharetrace;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import org.sharetrace.message.AlgorithmMessage;
import org.sharetrace.message.Run;

public class Runner {

  private Runner() {}

  public static void run(Behavior<AlgorithmMessage> algorithm) {
    ActorSystem.create(runner(algorithm), "Runner");
  }

  private static Behavior<Void> runner(Behavior<AlgorithmMessage> algorithm) {
    return Behaviors.setup(
        context -> {
          ActorRef<AlgorithmMessage> instance = context.spawn(algorithm, "Algorithm");
          context.watch(instance);
          instance.tell(Run.INSTANCE);
          return Behaviors.receive(Void.class)
              .onSignal(Terminated.class, signal -> Behaviors.stopped())
              .build();
        });
  }
}

package org.sharetrace;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.Behaviors;
import org.sharetrace.model.message.RiskPropMessage;
import org.sharetrace.model.message.Run;

public class Runner {

  private Runner() {}

  public static void run(Behavior<RiskPropMessage> riskPropagation) {
    ActorSystem.create(runner(riskPropagation), "Runner");
  }

  private static Behavior<Void> runner(Behavior<RiskPropMessage> riskPropagation) {
    return Behaviors.setup(
        context -> {
          ActorRef<RiskPropMessage> riskProp = context.spawn(riskPropagation, "RiskPropagation");
          context.watch(riskProp);
          riskProp.tell(Run.INSTANCE);
          return Behaviors.receive(Void.class)
              .onSignal(Terminated.class, signal -> Behaviors.stopped())
              .build();
        });
  }
}

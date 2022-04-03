package org.sharetrace;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import org.sharetrace.model.message.RiskPropagationMessage;
import org.sharetrace.model.message.Run;

public class Runner {

  private final Behavior<RiskPropagationMessage> riskPropagation;

  public Runner(Behavior<RiskPropagationMessage> riskPropagation) {
    this.riskPropagation = riskPropagation;
  }

  public Behavior<Void> run() {
    return Behaviors.setup(this::run);
  }

  private Behavior<Void> run(ActorContext<Void> context) {
    context.spawn(riskPropagation, "RiskPropagation").tell(Run.INSTANCE);
    // TODO How can we stop once activity has stopped?
    return Behaviors.receive(Void.class).build();
  }
}

package io.sharetrace.model.message;

import akka.actor.typed.ActorRef;
import org.immutables.value.Value;

@Value.Immutable
interface BaseThresholdMessage extends UserMessage {

  @Value.Parameter
  ActorRef<UserMessage> contact();
}

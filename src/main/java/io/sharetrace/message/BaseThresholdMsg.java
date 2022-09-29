package io.sharetrace.message;

import akka.actor.typed.ActorRef;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseThresholdMsg implements UserMsg {

  @Value.Parameter
  public abstract ActorRef<UserMsg> contact();
}

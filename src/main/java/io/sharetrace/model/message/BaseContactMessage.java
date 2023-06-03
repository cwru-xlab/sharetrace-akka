package io.sharetrace.model.message;

import akka.actor.typed.ActorRef;
import java.time.Instant;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseContactMessage implements UserMessage {

  @Value.Parameter
  public abstract ActorRef<UserMessage> contact();

  @Value.Parameter
  public abstract Instant timestamp();
}

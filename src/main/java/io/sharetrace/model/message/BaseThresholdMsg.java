package io.sharetrace.model.message;

import akka.actor.typed.ActorRef;
import org.immutables.value.Value;

@Value.Immutable
interface BaseThresholdMsg extends UserMsg {

    @Value.Parameter
    ActorRef<UserMsg> contact();
}

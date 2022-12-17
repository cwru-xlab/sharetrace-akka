package io.sharetrace.model.message;

import io.sharetrace.actor.UserActor;
import org.immutables.value.Value;

/**
 * A message that terminates a {@link UserActor} after a period of idleness.
 *
 * @see UserActor
 */
@Value.Immutable
interface BaseTimedOutMsg extends UserMsg, AlgorithmMsg {

    @Value.Parameter
    int user();
}

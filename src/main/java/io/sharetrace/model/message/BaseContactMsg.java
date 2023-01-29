package io.sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.google.common.collect.Range;
import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.actor.UserActor;
import io.sharetrace.graph.Contact;
import io.sharetrace.util.Checks;
import org.immutables.value.Value;

import java.time.Instant;

/**
 * A message that initiates message-passing {@link UserActor} with another {@link UserActor}.
 *
 * @see UserActor
 * @see RiskPropagation
 * @see Contact
 */
@Value.Immutable
abstract class BaseContactMsg implements UserMsg {

    private static final Range<Instant> TIME_RANGE = Range.atLeast(Contact.MIN_TIME);

    /**
     * Returns the actor reference of the contacted user.
     */
    @Value.Parameter
    public abstract ActorRef<UserMsg> contact();

    @Value.Check
    protected void check() {
        Checks.inRange(contactTime(), TIME_RANGE, "contactTime");
    }

    /**
     * Returns the time at which the two users came in contact.
     */
    @Value.Parameter
    public abstract Instant contactTime();
}

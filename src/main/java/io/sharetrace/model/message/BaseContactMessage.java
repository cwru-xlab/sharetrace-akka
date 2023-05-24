package io.sharetrace.model.message;

import akka.actor.typed.ActorRef;
import com.google.common.collect.Range;
import io.sharetrace.actor.RiskPropagation;
import io.sharetrace.actor.UserActor;
import io.sharetrace.graph.Contact;
import io.sharetrace.util.Checks;
import java.time.Instant;
import org.immutables.value.Value;

/**
 * A message that initiates message-passing {@link UserActor} with another {@link UserActor}.
 *
 * @see UserActor
 * @see RiskPropagation
 * @see Contact
 */
@Value.Immutable
abstract class BaseContactMessage implements UserMessage {

  private static final Range<Instant> TIME_RANGE = Range.atLeast(Contact.MIN_TIME);

  /** Returns the actor reference of the contacted user. */
  @Value.Parameter
  public abstract ActorRef<UserMessage> contact();

  @Value.Check
  protected void check() {
    Checks.checkRange(contactTime(), TIME_RANGE, "contactTime");
  }

  /** Returns the time at which the two users came in contact. */
  @Value.Parameter
  public abstract Instant contactTime();
}

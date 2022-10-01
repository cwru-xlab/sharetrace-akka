package io.sharetrace.message;

import io.sharetrace.actor.UserActor;

/**
 * A message that terminates a {@link UserActor} after a period of idleness.
 *
 * @see UserActor
 */
public enum TimeoutMsg implements UserMsg {
  INSTANCE
}

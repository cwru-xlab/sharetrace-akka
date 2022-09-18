package io.sharetrace.message;

import io.sharetrace.actor.User;

/**
 * A message that terminates a {@link User} after a period of idleness.
 *
 * @see User
 */
public enum TimeoutMsg implements UserMsg {
  INSTANCE
}

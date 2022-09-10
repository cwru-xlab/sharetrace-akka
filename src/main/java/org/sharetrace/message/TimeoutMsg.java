package org.sharetrace.message;

import org.sharetrace.actor.User;

/**
 * A message that terminates a {@link User} after a period of idleness.
 *
 * @see User
 */
public enum TimeoutMsg implements UserMsg {
  INSTANCE
}

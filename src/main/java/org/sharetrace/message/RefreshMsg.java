package org.sharetrace.message;

import org.sharetrace.actor.User;

/**
 * A message that refreshes a {@link User}.
 *
 * @see User
 */
public enum RefreshMsg implements UserMsg {
  INSTANCE
}

package io.sharetrace.message;

import io.sharetrace.actor.User;

/**
 * A message that refreshes a {@link User}.
 *
 * @see User
 */
public enum RefreshMsg implements UserMsg {
  INSTANCE
}

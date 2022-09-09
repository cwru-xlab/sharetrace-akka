package org.sharetrace.message;

import org.sharetrace.User;

/** A message that terminates a {@link User} after a period of idleness. */
public enum TimeoutMsg implements UserMsg {
  INSTANCE
}

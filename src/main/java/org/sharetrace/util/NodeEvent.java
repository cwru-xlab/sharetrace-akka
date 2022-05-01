package org.sharetrace.util;

public enum NodeEvent implements LoggableEvent {
  SEND_CACHED,
  SEND_CURRENT,
  PROPAGATE,
  RECEIVE,
  UPDATE,
  CURRENT_REFRESH,
  CONTACTS_REFRESH,
  NEW_CONTACT,
}

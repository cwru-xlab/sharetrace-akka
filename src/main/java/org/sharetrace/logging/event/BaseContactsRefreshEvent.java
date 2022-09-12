package org.sharetrace.logging.event;

import org.immutables.value.Value;

@Value.Immutable
interface BaseContactsRefreshEvent extends LoggableEvent {

  String user();

  int numRemaining();

  int numExpired();
}
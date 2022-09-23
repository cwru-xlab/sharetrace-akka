package io.sharetrace.logging.event;

import org.immutables.value.Value;

@Value.Immutable
interface BaseTimeoutEvent extends LoggableEvent {

  String user();
}

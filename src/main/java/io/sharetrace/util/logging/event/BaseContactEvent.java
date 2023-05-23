package io.sharetrace.util.logging.event;

import org.immutables.value.Value;

@Value.Immutable
interface BaseContactEvent extends LoggableEvent {

  String contact();
}

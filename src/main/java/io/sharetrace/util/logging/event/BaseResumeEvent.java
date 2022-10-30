package io.sharetrace.util.logging.event;

import org.immutables.value.Value;

@Value.Immutable
interface BaseResumeEvent extends LoggableEvent {

  String user();
}

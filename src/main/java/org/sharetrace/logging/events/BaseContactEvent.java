package org.sharetrace.logging.events;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
interface BaseContactEvent extends LoggableEvent {

  String of();

  List<String> nodes();
}

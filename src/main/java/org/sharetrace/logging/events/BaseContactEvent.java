package org.sharetrace.logging.events;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
interface BaseContactEvent extends LoggableEvent {

  String of();

  List<String> users();
}

package org.sharetrace.logging;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
interface BaseContactEvent extends LoggableEvent {

  String of();

  List<String> nodes();
}

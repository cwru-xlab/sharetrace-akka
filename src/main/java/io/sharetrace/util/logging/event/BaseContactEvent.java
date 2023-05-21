package io.sharetrace.util.logging.event;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
interface BaseContactEvent extends LoggableEvent {

  String user();

  List<String> users();
}

package io.sharetrace.util.logging.event;

import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
interface BaseContactEvent extends LoggableEvent {

    String user();

    List<String> users();
}

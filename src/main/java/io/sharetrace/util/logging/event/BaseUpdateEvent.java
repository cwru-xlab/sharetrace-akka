package io.sharetrace.util.logging.event;

import org.immutables.value.Value;

@Value.Immutable
interface BaseUpdateEvent extends ScoreChangeEvent {

  String from();

  String to();
}

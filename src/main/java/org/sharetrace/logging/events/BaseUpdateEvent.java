package org.sharetrace.logging.events;

import org.immutables.value.Value;

@Value.Immutable
interface BaseUpdateEvent extends ScoreChangeEvent {

  String from();

  String to();
}

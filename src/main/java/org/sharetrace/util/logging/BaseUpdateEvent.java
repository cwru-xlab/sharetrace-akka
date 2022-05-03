package org.sharetrace.util.logging;

import org.immutables.value.Value;

@Value.Immutable
interface BaseUpdateEvent extends ScoreChangeEvent {

  @Override
  @Value.Derived
  default String name() {
    return getClass().getSimpleName();
  }

  String from();

  String to();
}

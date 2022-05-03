package org.sharetrace.logging;

import org.immutables.value.Value;

@Value.Immutable
interface BaseSendCurrentEvent extends MessageEvent {

  @Override
  @Value.Derived
  default String name() {
    return getClass().getSimpleName();
  }
}

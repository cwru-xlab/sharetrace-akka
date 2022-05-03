package org.sharetrace.logging;

import org.immutables.value.Value;

@Value.Immutable
interface BasePropagateEvent extends MessageEvent {

  @Override
  @Value.Derived
  default String name() {
    return getClass().getSimpleName();
  }
}

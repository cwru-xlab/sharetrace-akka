package org.sharetrace.logging;

import org.immutables.value.Value;

@Value.Immutable
interface BaseContactsRefreshEvent extends LoggableEvent {

  @Override
  @Value.Derived
  default String name() {
    return getClass().getSimpleName();
  }

  String of();

  int nRemaining();

  int nExpired();
}

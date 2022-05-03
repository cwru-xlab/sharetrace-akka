package org.sharetrace.util.logging;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
interface BaseContactEvent extends LoggableEvent {

  @Override
  @Value.Derived
  default String name() {
    return getClass().getSimpleName();
  }

  String of();

  List<String> nodes();
}

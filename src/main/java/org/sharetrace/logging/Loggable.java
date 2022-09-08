package org.sharetrace.logging;

import org.immutables.value.Value;

public interface Loggable {

  @Value.Derived
  default String type() {
    return getClass().getSimpleName();
  }
}

package org.sharetrace.logging;

import org.immutables.value.Value;

public interface Loggable {

  @Value.Lazy
  default String type() {
    return getClass().getSimpleName();
  }
}

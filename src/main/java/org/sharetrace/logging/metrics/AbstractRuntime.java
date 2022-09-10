package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

public interface AbstractRuntime extends LoggableMetric {

  @Value.Parameter
  long ms();
}

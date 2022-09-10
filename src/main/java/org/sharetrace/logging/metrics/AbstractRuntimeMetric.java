package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

public interface AbstractRuntimeMetric extends LoggableMetric {

  @Value.Parameter
  float seconds();
}

package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseRuntimeMetric extends LoggableMetric {

  @Value.Parameter
  double seconds();
}

package org.sharetrace.logging;

import org.immutables.value.Value;

@Value.Immutable
interface BaseRuntimeMetric extends LoggableMetric {

  @Value.Parameter
  double seconds();
}

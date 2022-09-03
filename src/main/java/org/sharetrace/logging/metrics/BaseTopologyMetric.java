package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseTopologyMetric extends LoggableMetric {

  @Value.Parameter
  String label();
}

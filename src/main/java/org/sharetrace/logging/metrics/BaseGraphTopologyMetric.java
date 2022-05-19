package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphTopologyMetric extends LoggableMetric {

  @Value.Parameter
  String label();
}

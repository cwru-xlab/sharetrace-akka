package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphTopology extends LoggableMetric {

  @Value.Parameter
  String id();
}

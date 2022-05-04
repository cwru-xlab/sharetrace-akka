package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphSizeMetrics extends LoggableMetric {

  long nNodes();

  long nEdges();
}

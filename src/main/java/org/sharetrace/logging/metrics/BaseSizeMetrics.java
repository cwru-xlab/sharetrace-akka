package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseSizeMetrics extends LoggableMetric {

  long nNodes();

  long nEdges();
}

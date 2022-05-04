package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphCycleMetrics extends LoggableMetric {

  int girth();

  long nTriangles();
}

package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseCycleMetrics extends LoggableMetric {

  int girth();

  long nTriangles();
}

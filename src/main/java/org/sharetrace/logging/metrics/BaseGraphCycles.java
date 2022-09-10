package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphCycles extends LoggableMetric {

  int girth();

  long numTriangles();
}

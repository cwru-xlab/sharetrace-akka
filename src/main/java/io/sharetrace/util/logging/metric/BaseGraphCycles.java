package io.sharetrace.util.logging.metric;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphCycles extends LoggableMetric {

  int girth();

  long numTriangles();
}

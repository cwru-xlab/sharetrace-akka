package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphSize extends LoggableMetric {

  long numNodes();

  long numEdges();
}

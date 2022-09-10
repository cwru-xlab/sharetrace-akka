package org.sharetrace.logging.metric;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphSize extends LoggableMetric {

  long numNodes();

  long numEdges();
}

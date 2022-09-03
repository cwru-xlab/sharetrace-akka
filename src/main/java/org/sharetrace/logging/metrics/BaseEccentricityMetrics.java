package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseEccentricityMetrics extends LoggableMetric {

  int radius();

  int diameter();

  long center();

  long periphery();
}

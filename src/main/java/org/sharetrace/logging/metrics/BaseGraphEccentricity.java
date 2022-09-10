package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphEccentricity extends LoggableMetric {

  int radius();

  int diameter();

  long center();

  long periphery();
}

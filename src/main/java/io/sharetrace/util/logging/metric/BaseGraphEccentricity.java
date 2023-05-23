package io.sharetrace.util.logging.metric;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphEccentricity extends LoggableMetric {

  long radius();

  long diameter();

  long center();

  long periphery();
}

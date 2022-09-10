package org.sharetrace.logging.metric;

import org.immutables.value.Value;

public interface AbstractRuntime extends LoggableMetric {

  @Value.Parameter
  long ms();
}

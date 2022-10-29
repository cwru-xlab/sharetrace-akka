package io.sharetrace.logging.metric;

import org.immutables.value.Value;

interface RuntimeMetric extends LoggableMetric {

  @Value.Parameter
  long ms();
}

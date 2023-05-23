package io.sharetrace.util.logging.metric;

import java.time.Duration;
import org.immutables.value.Value;

interface RuntimeMetric extends LoggableMetric {

  @Value.Parameter
  Duration runtime();
}

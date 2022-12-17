package io.sharetrace.util.logging.metric;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphTopology extends LoggableMetric {

    @Value.Parameter
    String id();
}

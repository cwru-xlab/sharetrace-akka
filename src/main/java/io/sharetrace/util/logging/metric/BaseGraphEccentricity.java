package io.sharetrace.util.logging.metric;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphEccentricity extends LoggableMetric {

    int radius();

    int diameter();

    long center();

    long periphery();
}

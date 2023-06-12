package sharetrace.util.logging.metric;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphEccentricity extends MetricRecord {

  long radius();

  long diameter();

  long center();

  long periphery();
}

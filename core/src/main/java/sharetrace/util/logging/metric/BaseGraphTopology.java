package sharetrace.util.logging.metric;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphTopology extends MetricRecord {

  @Value.Parameter
  String id();
}

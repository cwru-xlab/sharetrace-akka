package sharetrace.util.logging.metric;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphSize extends MetricRecord {

  long vertices();

  long edges();
}

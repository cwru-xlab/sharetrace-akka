package sharetrace.util.logging.metric;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphCycles extends MetricRecord {

  long girth();

  long triangles();
}

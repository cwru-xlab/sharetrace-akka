package sharetrace.util.logging.metric;

import org.immutables.value.Value;
import sharetrace.model.Identifiable;

@Value.Immutable
interface BaseGraphTopology extends MetricRecord, Identifiable {

  @Value.Parameter
  long id();
}

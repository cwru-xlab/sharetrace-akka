package sharetrace.logging.metric;

import sharetrace.graph.ContactNetwork;
import sharetrace.model.Identifiable;

public record GraphTopology(String id) implements MetricRecord, Identifiable {

  public static GraphTopology of(ContactNetwork<?> network) {
    return new GraphTopology(network.id());
  }
}

package sharetrace.logging.metric;

import sharetrace.graph.TemporalNetwork;
import sharetrace.model.Identifiable;

public record GraphTopology(String id) implements MetricRecord, Identifiable {

  public static GraphTopology of(TemporalNetwork<?> network) {
    return new GraphTopology(network.id());
  }
}

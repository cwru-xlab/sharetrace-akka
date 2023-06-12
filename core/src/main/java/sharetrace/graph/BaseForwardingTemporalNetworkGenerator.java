package sharetrace.graph;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;

@Value.Immutable
abstract class BaseForwardingTemporalNetworkGenerator<V>
    implements GraphGenerator<V, TemporalEdge, V> {

  public abstract GraphGenerator<V, TemporalEdge, ?> delegate();

  public abstract Optional<BiFunction<V, V, Instant>> timeFactory();

  @Override
  public void generateGraph(Graph<V, TemporalEdge> target, Map<String, V> resultMap) {
    delegate().generateGraph(target);
    timeFactory().ifPresent(timeFactory -> setTimestamps(target, timeFactory));
  }

  private void setTimestamps(Graph<V, TemporalEdge> target, BiFunction<V, V, Instant> timeFactory) {
    for (TemporalEdge edge : target.edgeSet()) {
      V v1 = target.getEdgeSource(edge);
      V v2 = target.getEdgeTarget(edge);
      edge.setTimestamp(timeFactory.apply(v1, v2));
    }
  }
}

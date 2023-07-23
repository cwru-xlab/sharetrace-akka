package sharetrace.graph;

import java.util.Set;
import org.jgrapht.Graph;
import sharetrace.model.Identifiable;

public interface TemporalNetwork<V> extends Graph<V, TemporalEdge>, Identifiable {

  @Override
  TemporalEdge addEdge(V source, V target);

  @Override
  boolean addEdge(V source, V target, TemporalEdge edge);

  default Set<V> nodeSet() {
    return vertexSet();
  }
}

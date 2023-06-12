package sharetrace.graph;

import java.util.Set;
import org.jgrapht.Graph;
import sharetrace.model.Identifiable;

public interface TemporalNetwork<V> extends Graph<V, TemporalEdge>, Identifiable {

  String type();

  default Set<V> nodeSet() {
    return vertexSet();
  }

  default boolean addNode(V node) {
    return addVertex(node);
  }

  @Override
  TemporalEdge addEdge(V source, V target);

  @Override
  boolean addEdge(V source, V target, TemporalEdge edge);
}

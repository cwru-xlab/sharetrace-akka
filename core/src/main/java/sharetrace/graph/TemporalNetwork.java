package sharetrace.graph;

import org.jgrapht.Graph;
import sharetrace.model.Identifiable;

public interface TemporalNetwork<V> extends Graph<V, TemporalEdge>, Identifiable {

  String type();
}

package sharetrace.graph;

import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;

public interface TemporalNetworkFactory<V> {

  Graph<V, TemporalEdge> newTarget();

  GraphGenerator<V, TemporalEdge, ?> graphGenerator();

  TemporalNetwork<V> newNetwork(Graph<V, TemporalEdge> target);

  default TemporalNetwork<V> getNetwork() {
    var target = newTarget();
    graphGenerator().generateGraph(target);
    return newNetwork(target);
  }
}

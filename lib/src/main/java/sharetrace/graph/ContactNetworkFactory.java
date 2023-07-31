package sharetrace.graph;

import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;

public interface ContactNetworkFactory<V> {

  Graph<V, TemporalEdge> newTarget();

  GraphGenerator<V, TemporalEdge, ?> graphGenerator();

  ContactNetwork<V> newContactNetwork(Graph<V, TemporalEdge> target);

  default ContactNetwork<V> getContactNetwork() {
    var target = newTarget();
    graphGenerator().generateGraph(target);
    return newContactNetwork(target);
  }
}

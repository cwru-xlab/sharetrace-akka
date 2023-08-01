package sharetrace.graph;

import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;

public interface ContactNetworkFactory {

  default Graph<Integer, TemporalEdge> newTarget() {
    return Graphs.newTemporalGraph();
  }

  GraphGenerator<Integer, TemporalEdge, ?> graphGenerator();

  ContactNetwork newContactNetwork(Graph<Integer, TemporalEdge> target);

  default ContactNetwork getContactNetwork() {
    var target = newTarget();
    graphGenerator().generateGraph(target);
    return newContactNetwork(target);
  }
}

package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.model.graph.ContactNetwork;
import sharetrace.model.graph.Graphs;
import sharetrace.model.graph.SimpleContactNetwork;
import sharetrace.model.graph.TemporalEdge;

public interface ContactNetworkFactory {

  @JsonProperty
  String type();

  default Graph<Integer, TemporalEdge> newTarget() {
    return Graphs.newTemporalGraph();
  }

  GraphGenerator<Integer, TemporalEdge, ?> graphGenerator();

  default ContactNetwork newContactNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleContactNetwork(IdFactory.newId(), target);
  }

  @JsonIgnore
  default ContactNetwork getContactNetwork() {
    var target = newTarget();
    graphGenerator().generateGraph(target);
    return newContactNetwork(target);
  }
}

package sharetrace.model.factory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.model.graph.ContactNetwork;
import sharetrace.model.graph.Graphs;
import sharetrace.model.graph.SimpleContactNetwork;
import sharetrace.model.graph.TemporalEdge;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ContactNetworkFactory {

  @JsonIgnore
  default Graph<Integer, TemporalEdge> newTarget() {
    return Graphs.newTemporalGraph();
  }

  @JsonIgnore
  GraphGenerator<Integer, TemporalEdge, ?> graphGenerator();

  default ContactNetwork newContactNetwork(String id, Graph<Integer, TemporalEdge> target) {
    return new SimpleContactNetwork(id, target);
  }

  @JsonIgnore
  default ContactNetwork getContactNetwork() {
    var target = newTarget();
    graphGenerator().generateGraph(target);
    return newContactNetwork(IdFactory.newId(), target);
  }
}

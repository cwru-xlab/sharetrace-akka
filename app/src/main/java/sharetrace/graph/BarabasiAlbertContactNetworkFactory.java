package sharetrace.graph;

import java.util.Map;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.Graph;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.TimeFactory;

@Buildable
public record BarabasiAlbertContactNetworkFactory(
    int nodes,
    int initialNodes,
    int newEdges,
    TimeFactory timeFactory,
    RandomGenerator randomGenerator)
    implements GeneratedContactNetworkFactory {

  @Override
  public GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    var random = RandomAdaptor.createAdaptor(randomGenerator);
    return new BarabasiAlbertGraphGenerator<>(initialNodes, newEdges, nodes, random);
  }

  @Override
  public ContactNetwork newContactNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleContactNetwork(target, "BarabasiAlbert", props());
  }

  private Map<String, ?> props() {
    return Map.of("nodes", nodes, "initialNodes", initialNodes, "newEdges", newEdges);
  }
}

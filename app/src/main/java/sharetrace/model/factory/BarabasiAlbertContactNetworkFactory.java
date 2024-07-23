package sharetrace.model.factory;

import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.graph.TemporalEdge;

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
  public String type() {
    return "BarabasiAlbert";
  }
}

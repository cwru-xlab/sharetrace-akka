package sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.GeneratedContactNetworkFactory;
import sharetrace.model.factory.TimeFactory;
import sharetrace.model.graph.TemporalEdge;

@JsonTypeName("BarabasiAlbert")
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
}

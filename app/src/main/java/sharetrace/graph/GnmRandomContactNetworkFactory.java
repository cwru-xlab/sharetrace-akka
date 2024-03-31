package sharetrace.graph;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.GeneratedContactNetworkFactory;
import sharetrace.model.factory.TimeFactory;
import sharetrace.model.graph.TemporalEdge;

@JsonTypeName("GnmRandom")
@Buildable
public record GnmRandomContactNetworkFactory(
    int nodes,
    int edges,
    boolean loops,
    boolean multipleEdges,
    TimeFactory timeFactory,
    RandomGenerator randomGenerator)
    implements GeneratedContactNetworkFactory {

  @Override
  public GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    var random = RandomAdaptor.createAdaptor(randomGenerator);
    return new GnmRandomGraphGenerator<>(nodes, edges, random, loops, multipleEdges);
  }
}

package sharetrace.graph;

import java.util.Map;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.Graph;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.TimeFactory;

@Buildable
public record GnmRandomContactNetworkFactory(
    int nodes, int edges, TimeFactory timeFactory, RandomGenerator randomGenerator)
    implements GeneratedContactNetworkFactory {

  private static final boolean LOOPS = false;
  private static final boolean MULTIPLE_EDGES = false;

  @Override
  public GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    var random = RandomAdaptor.createAdaptor(randomGenerator);
    return new GnmRandomGraphGenerator<>(nodes, edges, random, LOOPS, MULTIPLE_EDGES);
  }

  @Override
  public ContactNetwork newContactNetwork(Graph<Integer, TemporalEdge> target) {
    var props = Map.of("loops", LOOPS, "multipleEdges", MULTIPLE_EDGES);
    return new SimpleContactNetwork(target, "GnmRandom", props);
  }
}

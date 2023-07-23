package sharetrace.graph;

import java.util.Map;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.Graph;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.TimeFactory;
import sharetrace.util.IdFactory;

@Buildable
public record GnmRandomTemporalNetworkFactory(
    int nodes, int edges, TimeFactory timeFactory, RandomGenerator randomGenerator)
    implements GeneratedTemporalNetworkFactory<Integer> {

  private static final boolean LOOPS = false;
  private static final boolean MULTIPLE_EDGES = false;

  @Override
  public Graph<Integer, TemporalEdge> newTarget() {
    return GraphFactory.newIntGraph();
  }

  @Override
  public GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    var random = RandomAdaptor.createAdaptor(randomGenerator);
    return new GnmRandomGraphGenerator<>(nodes, edges, random, LOOPS, MULTIPLE_EDGES);
  }

  @Override
  public TemporalNetwork<Integer> newNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleTemporalNetwork<>(target, IdFactory.nextUlid(), "GnmRandom", properties());
  }

  private Map<String, ?> properties() {
    return Map.of("nodes", nodes, "edges", edges, "loops", LOOPS, "multipleEdges", MULTIPLE_EDGES);
  }
}

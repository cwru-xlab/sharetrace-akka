package sharetrace.graph;

import java.util.Map;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.RandomRegularGraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.TimeFactory;
import sharetrace.util.IdFactory;

@Buildable
public record RandomRegularTemporalNetworkFactory(
    int nodes, int degree, TimeFactory timeFactory, RandomGenerator randomGenerator)
    implements GeneratedTemporalNetworkFactory<Integer> {

  @Override
  public Graph<Integer, TemporalEdge> newTarget() {
    return GraphFactory.newIntGraph();
  }

  @Override
  public GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    var random = RandomAdaptor.createAdaptor(randomGenerator);
    return new RandomRegularGraphGenerator<>(nodes, degree, random);
  }

  @Override
  public TemporalNetwork<Integer> newNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleTemporalNetwork<>(target, IdFactory.nextUlid(), "RandomRegular", properties());
  }

  private Map<String, ?> properties() {
    return Map.of("nodes", nodes, "degree", degree);
  }
}

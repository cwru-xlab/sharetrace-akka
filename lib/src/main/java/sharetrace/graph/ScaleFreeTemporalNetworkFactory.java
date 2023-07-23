package sharetrace.graph;

import java.util.Map;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.ScaleFreeGraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.TimeFactory;
import sharetrace.util.IdFactory;

@Buildable
public record ScaleFreeTemporalNetworkFactory(
    int nodes, TimeFactory timeFactory, RandomGenerator randomGenerator)
    implements GeneratedTemporalNetworkFactory<Integer> {

  @Override
  public Graph<Integer, TemporalEdge> newTarget() {
    return GraphFactory.newIntGraph();
  }

  @Override
  public GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    return new ScaleFreeGraphGenerator<>(nodes, RandomAdaptor.createAdaptor(randomGenerator));
  }

  @Override
  public TemporalNetwork<Integer> newNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleTemporalNetwork<>(target, IdFactory.nextUlid(), "ScaleFree", properties());
  }

  private Map<String, ?> properties() {
    return Map.of("nodes", nodes());
  }
}

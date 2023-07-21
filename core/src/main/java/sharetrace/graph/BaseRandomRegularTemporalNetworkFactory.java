package sharetrace.graph;

import java.util.Map;
import java.util.Random;
import org.apache.commons.math3.random.RandomAdaptor;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.RandomRegularGraphGenerator;
import sharetrace.util.IdFactory;

@Value.Immutable
abstract class BaseRandomRegularTemporalNetworkFactory
    extends GeneratedTemporalNetworkFactory<Integer> {

  public abstract int degree();

  @Override
  protected Graph<Integer, TemporalEdge> newTarget() {
    return GraphFactory.newIntGraph();
  }

  @Override
  protected GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    Random random = RandomAdaptor.createAdaptor(random());
    return new RandomRegularGraphGenerator<>(nodes(), degree(), random);
  }

  @Override
  protected TemporalNetwork<Integer> newNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleTemporalNetwork<>(target, IdFactory.newInt(), "RandomRegular", properties());
  }

  private Map<String, ?> properties() {
    return Map.of("nodes", nodes(), "degree", degree());
  }
}

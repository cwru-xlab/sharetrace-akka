package sharetrace.graph;

import java.util.Map;
import java.util.Random;
import org.apache.commons.math3.random.RandomAdaptor;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.ScaleFreeGraphGenerator;
import sharetrace.util.IdFactory;

@Value.Immutable
abstract class BaseScaleFreeTemporalNetworkFactory
    extends GeneratedTemporalNetworkFactory<Integer> {

  protected Graph<Integer, TemporalEdge> newTarget() {
    return GraphFactory.newIntGraph();
  }

  protected GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    Random random = RandomAdaptor.createAdaptor(random());
    return new ScaleFreeGraphGenerator<>(nodes(), random);
  }

  protected TemporalNetwork<Integer> newNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleTemporalNetwork<>(target, IdFactory.newIntString(), "ScaleFree", properties());
  }

  private Map<String, ?> properties() {
    return Map.of("nodes", nodes());
  }
}

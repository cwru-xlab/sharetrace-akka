package sharetrace.graph;

import java.util.Map;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.ScaleFreeGraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.TimeFactory;

@Buildable
public record ScaleFreeContactNetworkFactory(
    int nodes, TimeFactory timeFactory, RandomGenerator randomGenerator)
    implements GeneratedContactNetworkFactory {

  @Override
  public GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    return new ScaleFreeGraphGenerator<>(nodes, RandomAdaptor.createAdaptor(randomGenerator));
  }

  @Override
  public ContactNetwork newContactNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleContactNetwork(target, "ScaleFree", props());
  }

  private Map<String, ?> props() {
    return Map.of("nodes", nodes());
  }
}

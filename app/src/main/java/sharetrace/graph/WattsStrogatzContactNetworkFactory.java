package sharetrace.graph;

import java.util.Map;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.TimeFactory;

@Buildable
public record WattsStrogatzContactNetworkFactory(
    int nodes,
    int nearestNeighbors,
    double rewiringProbability,
    TimeFactory timeFactory,
    RandomGenerator randomGenerator)
    implements GeneratedContactNetworkFactory {

  @Override
  public GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    var random = RandomAdaptor.createAdaptor(randomGenerator);
    var addInsteadOfRewire = false;
    return new WattsStrogatzGraphGenerator<>(
        nodes, nearestNeighbors, rewiringProbability, addInsteadOfRewire, random);
  }

  @Override
  public ContactNetwork newContactNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleContactNetwork(target, "WattsStrogatz", props());
  }

  private Map<String, ?> props() {
    return Map.of("nearestNeighbors", nearestNeighbors, "rewiringProbability", rewiringProbability);
  }
}

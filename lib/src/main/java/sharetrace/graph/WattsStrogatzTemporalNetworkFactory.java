package sharetrace.graph;

import java.util.Map;
import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import sharetrace.Buildable;
import sharetrace.model.factory.TimeFactory;
import sharetrace.util.IdFactory;

@Buildable
public record WattsStrogatzTemporalNetworkFactory(
    int nodes,
    int nearestNeighbors,
    double rewiringProbability,
    TimeFactory timeFactory,
    RandomGenerator randomGenerator)
    implements GeneratedTemporalNetworkFactory<Integer> {

  @Override
  public Graph<Integer, TemporalEdge> newTarget() {
    return GraphFactory.newIntGraph();
  }

  @Override
  public GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    var random = RandomAdaptor.createAdaptor(randomGenerator);
    var addInsteadOfRewire = false;
    return new WattsStrogatzGraphGenerator<>(
        nodes, nearestNeighbors, rewiringProbability, addInsteadOfRewire, random);
  }

  @Override
  public TemporalNetwork<Integer> newNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleTemporalNetwork<>(target, IdFactory.nextUlid(), "WattsStrogatz", properties());
  }

  private Map<String, ?> properties() {
    return Map.ofEntries(
        Map.entry("nodes", nodes),
        Map.entry("nearestNeighbors", nearestNeighbors),
        Map.entry("rewiringProbability", rewiringProbability));
  }
}

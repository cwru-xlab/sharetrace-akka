package sharetrace.graph;

import java.util.Map;
import java.util.Random;
import org.apache.commons.math3.random.RandomAdaptor;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import sharetrace.util.IdFactory;

@Value.Immutable
abstract class BaseWattsStrogatzTemporalNetworkFactory
    extends GeneratedTemporalNetworkFactory<Integer> {

  public abstract int nearestNeighbors();

  public abstract double rewiringProbability();

  @Override
  protected Graph<Integer, TemporalEdge> newTarget() {
    return GraphFactory.newIntGraph();
  }

  @Override
  protected GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    Random random = RandomAdaptor.createAdaptor(random());
    boolean addInsteadOfRewire = false;
    return new WattsStrogatzGraphGenerator<>(
        nodes(), nearestNeighbors(), rewiringProbability(), addInsteadOfRewire, random);
  }

  @Override
  protected TemporalNetwork<Integer> newNetwork(Graph<Integer, TemporalEdge> target) {
    return new SimpleTemporalNetwork<>(target, IdFactory.newInt(), "WattsStrogatz", properties());
  }

  private Map<String, ?> properties() {
    return Map.ofEntries(
        Map.entry("nodes", nodes()),
        Map.entry("nearestNeighbors", nearestNeighbors()),
        Map.entry("rewiringProbability", rewiringProbability()));
  }
}

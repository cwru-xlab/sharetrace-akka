package sharetrace.graph;

import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;

public abstract class AbstractTemporalNetworkFactory<V> implements TemporalNetworkFactory<V> {

  @Override
  public TemporalNetwork<V> getNetwork() {
    Graph<V, TemporalEdge> target = newTarget();
    graphGenerator().generateGraph(target);
    return newNetwork(target);
  }

  protected abstract Graph<V, TemporalEdge> newTarget();

  protected abstract GraphGenerator<V, TemporalEdge, V> graphGenerator();

  protected abstract TemporalNetwork<V> newNetwork(Graph<V, TemporalEdge> target);
}

package io.sharetrace.graph;

import org.apache.commons.math3.random.RandomGenerator;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;

abstract class AbstractTemporalNetworkFactory<V> implements TemporalNetworkFactory<V> {

  public abstract RandomGenerator random();

  public abstract int vertices();

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

package io.sharetrace.graph;

import io.sharetrace.util.Identifiers;
import java.util.Random;
import org.apache.commons.math3.random.RandomAdaptor;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.GraphGenerator;

@Value.Immutable
abstract class BaseBarabasiAlbertTemporalNetworkFactory
    extends AbstractTemporalNetworkFactory<Integer> {

  public abstract int initialVertices();

  public abstract int vertices();

  public abstract int newEdges();

  @Override
  protected Graph<Integer, TemporalEdge> newTarget() {
    return TemporalNetworkFactoryHelper.newIntTarget();
  }

  @Override
  protected GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    Random random = RandomAdaptor.createAdaptor(random());
    return new BarabasiAlbertGraphGenerator<>(initialVertices(), newEdges(), vertices(), random);
  }

  @Override
  protected TemporalNetwork<Integer> newNetwork(Graph<Integer, TemporalEdge> target) {
    return new ForwardingTemporalNetwork<>(target, Identifiers.newIntString(), "BarabasiAlbert");
  }
}

package io.sharetrace.graph;

import io.sharetrace.util.Identifiers;
import java.util.Random;
import org.apache.commons.math3.random.RandomAdaptor;
import org.immutables.value.Value;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.ScaleFreeGraphGenerator;

@Value.Immutable
abstract class BaseScaleFreeTemporalNetworkFactory extends AbstractTemporalNetworkFactory<Integer> {

  protected Graph<Integer, TemporalEdge> newTarget() {
    return TemporalNetworkFactoryHelper.newIntTarget();
  }

  protected GraphGenerator<Integer, TemporalEdge, Integer> graphGenerator() {
    Random random = RandomAdaptor.createAdaptor(random());
    return new ScaleFreeGraphGenerator<>(vertices(), random);
  }

  protected TemporalNetwork<Integer> newNetwork(Graph<Integer, TemporalEdge> target) {
    return new ForwardingTemporalNetwork<>(target, Identifiers.newIntString(), "ScaleFree");
  }
}

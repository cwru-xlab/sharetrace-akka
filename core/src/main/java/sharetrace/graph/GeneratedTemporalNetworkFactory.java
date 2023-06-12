package sharetrace.graph;

import org.apache.commons.math3.random.RandomGenerator;
import sharetrace.experiment.data.TimestampFactory;

public abstract class GeneratedTemporalNetworkFactory<V> extends AbstractTemporalNetworkFactory<V> {

  public abstract RandomGenerator random();

  public abstract TimestampFactory timestampFactory();

  public abstract int nodes();

  @Override
  public TemporalNetwork<V> getNetwork() {
    TemporalNetwork<V> network = super.getNetwork();
    network.edgeSet().forEach(edge -> edge.setTimestamp(timestampFactory().getTimestamp()));
    return network;
  }
}

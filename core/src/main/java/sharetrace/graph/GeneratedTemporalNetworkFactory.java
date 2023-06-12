package sharetrace.graph;

import org.apache.commons.math3.random.RandomGenerator;

public abstract class GeneratedTemporalNetworkFactory
    extends AbstractTemporalNetworkFactory<Integer> {

  public abstract RandomGenerator random();

  public abstract int vertices();
}

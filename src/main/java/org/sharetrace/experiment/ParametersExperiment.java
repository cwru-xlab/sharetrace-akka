package org.sharetrace.experiment;

import java.util.Random;

public final class ParametersExperiment extends SyntheticExperiment {

  private double sendTolerance;
  private double transmissionRate;

  public ParametersExperiment(GraphType graphType, int nNodes) {
    this(graphType, nNodes, 1);
  }

  public ParametersExperiment(GraphType graphType, int nNodes, int nRepeats) {
    this(graphType, nNodes, nRepeats, new Random().nextLong());
  }

  public ParametersExperiment(GraphType graphType, int nNodes, int nRepeats, long seed) {
    super(graphType, seed, nRepeats);
    this.nNodes = nNodes;
  }

  public ParametersExperiment(GraphType graphType, int nNodes, long seed) {
    this(graphType, nNodes, 1, seed);
  }

  @Override
  public void run() {
    for (transmissionRate = 0.1d; transmissionRate < 1d; transmissionRate += 0.1d) {
      for (sendTolerance = 0.1d; sendTolerance < 1.1d; sendTolerance += 0.1d) {
        super.run();
      }
    }
  }

  @Override
  protected double sendTolerance() {
    return sendTolerance;
  }

  @Override
  protected double transmissionRate() {
    return transmissionRate;
  }
}

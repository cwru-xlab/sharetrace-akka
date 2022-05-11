package org.sharetrace.experiment;

public final class ParametersExperiment extends SyntheticExperiment {

  private double sendTolerance;
  private double transmissionRate;

  public ParametersExperiment(GraphType graphType, int nNodes, int nRepeats, long seed) {
    super(graphType, seed, nRepeats);
    this.nNodes = nNodes;
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

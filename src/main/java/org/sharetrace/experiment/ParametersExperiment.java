package org.sharetrace.experiment;

import java.util.Random;

public final class ParametersExperiment extends SyntheticExperiment {

  private double sendTolerance;
  private double transmissionRate;

  public ParametersExperiment(GraphType graphType, int nNodes, int nRepeats, long seed) {
    super(graphType, seed, nRepeats);
    this.nNodes = nNodes;
  }

  public static void run(GraphType graphType, int nNodes) {
    run(graphType, nNodes, 1);
  }

  public static void run(GraphType graphType, int nNodes, int nRepeats) {
    run(graphType, nNodes, nRepeats, new Random().nextLong());
  }

  public static void run(GraphType graphType, int nNodes, int nRepeats, long seed) {
    new ParametersExperiment(graphType, nNodes, nRepeats, seed).run();
  }

  public static void run(GraphType graphType, int nNodes, long seed) {
    run(graphType, nNodes, 1, seed);
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

package org.sharetrace.experiment;

import java.util.Random;

public final class ParametersExperiment extends SyntheticExperiment {

  private float sendCoefficient;
  private float transmissionRate;

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

  @Override
  public void run() {
    for (transmissionRate = 0.1f; transmissionRate < 1d; transmissionRate += 0.1d) {
      for (sendCoefficient = 0.1f; sendCoefficient < 1.1d; sendCoefficient += 0.1d) {
        super.run();
      }
    }
  }

  @Override
  protected float sendCoefficient() {
    return sendCoefficient;
  }

  @Override
  protected float transmissionRate() {
    return transmissionRate;
  }

  public static void run(GraphType graphType, int nNodes, long seed) {
    run(graphType, nNodes, 1, seed);
  }
}

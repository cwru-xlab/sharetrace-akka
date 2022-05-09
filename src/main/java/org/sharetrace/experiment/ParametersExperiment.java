package org.sharetrace.experiment;

import java.util.stream.IntStream;

public final class ParametersExperiment extends SyntheticExperiment {

  private final double minTransmissionRate;
  private final double maxTransmissionRate;
  private final double stepTransmissionRate;
  private final double minSendTolerance;
  private final double maxSendTolerance;
  private final double stepSendTolerance;
  private final int nRepeats;
  private double sendTolerance;
  private double transmissionRate;

  public ParametersExperiment(
      GraphType graphType,
      int nNodes,
      int nEdges,
      double minTransmissionRate,
      double maxTransmissionRate,
      double stepTransmissionRate,
      double minSendTolerance,
      double maxSendTolerance,
      double stepSendTolerance,
      int nRepeats,
      long seed) {
    super(graphType, nNodes, nEdges, seed);
    this.minTransmissionRate = minTransmissionRate;
    this.maxTransmissionRate = maxTransmissionRate;
    this.stepTransmissionRate = stepTransmissionRate;
    this.minSendTolerance = minSendTolerance;
    this.maxSendTolerance = maxSendTolerance;
    this.stepSendTolerance = stepSendTolerance;
    this.nRepeats = nRepeats;
  }

  @Override
  public void run() {
    for (double tr = minTransmissionRate; tr < maxTransmissionRate; tr += stepTransmissionRate) {
      transmissionRate = tr;
      for (double st = minSendTolerance; st < maxSendTolerance; st += stepSendTolerance) {
        sendTolerance = st;
        IntStream.range(0, nRepeats).forEach(x -> super.run());
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

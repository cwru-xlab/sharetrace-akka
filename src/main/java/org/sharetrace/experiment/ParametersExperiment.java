package org.sharetrace.experiment;

import java.util.Objects;
import org.sharetrace.util.Range;

public class ParametersExperiment extends SyntheticExperiment {

  private final Range transmissionRates;
  private final Range sendCoefficients;
  private float sendCoefficient;
  private float transmissionRate;

  protected ParametersExperiment(Builder builder) {
    super(builder);
    this.transmissionRates = builder.transmissionRates;
    this.sendCoefficients = builder.sendCoefficients;
  }

  @Override
  public void run() {
    for (int tr : transmissionRates) {
      transmissionRate = scale(tr);
      for (int sc : sendCoefficients) {
        sendCoefficient = scale(sc);
        super.run();
      }
    }
  }

  private static float scale(float value) {
    return value / 10f;
  }

  @Override
  protected float sendCoefficient() {
    return sendCoefficient;
  }

  @Override
  protected float transmissionRate() {
    return transmissionRate;
  }

  public static class Builder extends SyntheticExperiment.Builder {
    private Integer nNodes;
    private Range transmissionRates = Range.of(1, 10);
    private Range sendCoefficients = Range.of(1, 11);

    public Builder nNodes(int nNodes) {
      this.nNodes = nNodes;
      return this;
    }

    public Builder transmissionRates(Range transmissionRates) {
      this.transmissionRates = Objects.requireNonNull(transmissionRates);
      return this;
    }

    public Builder sendCoefficients(Range sendCoefficients) {
      this.sendCoefficients = Objects.requireNonNull(sendCoefficients);
      return this;
    }

    @Override
    public void preBuild() {
      Objects.requireNonNull(nNodes);
      super.preBuild();
    }

    @Override
    public Experiment build() {
      preBuild();
      return new ParametersExperiment(this);
    }
  }
}

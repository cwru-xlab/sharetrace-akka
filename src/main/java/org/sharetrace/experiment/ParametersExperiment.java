package org.sharetrace.experiment;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.metrics.GraphTopologyMetric;
import org.sharetrace.util.Range;

public class ParametersExperiment extends SyntheticExperiment {

  private final Range transmissionRates;
  private final Range sendCoefficients;
  private float sendCoefficient;
  private float transmissionRate;

  protected ParametersExperiment(Builder builder) {
    super(builder);
    this.nNodes = builder.nNodes;
    this.transmissionRates = builder.transmissionRates;
    this.sendCoefficients = builder.sendCoefficients;
  }

  public static Builder builder() {
    return new Builder();
  }

  protected Set<Class<? extends Loggable>> loggable() {
    return super.loggable().stream()
        .filter(loggable -> !loggable.equals(GraphTopologyMetric.class))
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public void run() {
    for (double tr : transmissionRates) {
      transmissionRate = (float) tr;
      for (double sc : sendCoefficients) {
        sendCoefficient = (float) sc;
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

  public static class Builder extends SyntheticExperiment.Builder {

    private Integer nNodes;
    private Range transmissionRates = Range.of(1, 10, 1, 0.1);
    private Range sendCoefficients = Range.of(1, 11, 1, 0.1);

    public Builder nNodes(int nNodes) {
      this.nNodes = nNodes;
      return this;
    }

    public Builder transmissionRates(Range transmissionRates) {
      this.transmissionRates = transmissionRates;
      return this;
    }

    public Builder sendCoefficients(Range sendCoefficients) {
      this.sendCoefficients = sendCoefficients;
      return this;
    }

    @Override
    public Experiment build() {
      preBuild();
      return new ParametersExperiment(this);
    }

    @Override
    protected void preBuild() {
      Objects.requireNonNull(nNodes);
      Objects.requireNonNull(transmissionRates);
      Objects.requireNonNull(sendCoefficients);
      super.preBuild();
    }
  }
}

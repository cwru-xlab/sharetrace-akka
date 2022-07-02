package org.sharetrace.experiment;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.SyntheticDataset;
import org.sharetrace.data.factory.GraphGeneratorBuilder;
import org.sharetrace.data.factory.GraphGeneratorFactory;
import org.sharetrace.data.sampling.RiskScoreSampler;
import org.sharetrace.data.sampling.Sampler;
import org.sharetrace.data.sampling.TimeSampler;
import org.sharetrace.graph.Edge;
import org.sharetrace.message.RiskScore;

public class SyntheticExperiment extends Experiment {

  private final Sampler<RiskScore> riskScoreSampler;
  private final Sampler<Instant> contactTimeSampler;
  private final GraphGeneratorFactory graphGeneratorFactory;
  protected int nNodes = -1;

  protected SyntheticExperiment(Builder builder) {
    super(builder);
    this.graphGeneratorFactory = builder.generatorFactory;
    this.riskScoreSampler = newRiskScoreSampler();
    this.contactTimeSampler = newContactTimeSampler();
  }

  protected Sampler<RiskScore> newRiskScoreSampler() {
    Sampler<Instant> timeSampler = newTimeSampler(scoreTtl());
    return RiskScoreSampler.builder().timeSampler(timeSampler).seed(seed).build();
  }

  protected Sampler<Instant> newContactTimeSampler() {
    return newTimeSampler(contactTtl());
  }

  protected Sampler<Instant> newTimeSampler(Duration ttl) {
    return TimeSampler.builder().seed(seed).referenceTime(referenceTime).ttl(ttl).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected Dataset newDataset() {
    return SyntheticDataset.builder()
        .addAllLoggable(loggable())
        .graphGenerator(graphGeneratorFactory.getGraphGenerator(nNodes))
        .riskScoreFactory(x -> riskScoreSampler.sample())
        .contactTimeFactory((x, xx) -> contactTimeSampler.sample())
        .build();
  }

  public static class Builder extends Experiment.Builder {

    private GraphGeneratorFactory generatorFactory;

    public Builder graphGeneratorFactory(GraphGeneratorFactory factory) {
      this.generatorFactory = Objects.requireNonNull(factory);
      return this;
    }

    @Override
    public void preBuild() {
      generatorFactory = Objects.requireNonNullElseGet(generatorFactory, this::defaultFactory);
      super.preBuild();
    }

    @Override
    public Experiment build() {
      preBuild();
      return new SyntheticExperiment(this);
    }

    protected GraphGeneratorFactory defaultFactory() {
      return nNodes ->
          GraphGeneratorBuilder.<Integer, Edge<Integer>>create(graphType, nNodes, seed)
              .nEdges(nNodes * 2)
              .degree(4)
              .kNearestNeighbors(2)
              .nInitialNodes(2)
              .nNewEdges(2)
              .rewiringProbability(0.3)
              .build();
    }
  }
}

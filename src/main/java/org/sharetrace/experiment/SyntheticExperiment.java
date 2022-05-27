package org.sharetrace.experiment;

import java.time.Duration;
import java.time.Instant;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.RiskScoreFactory;
import org.sharetrace.data.factory.SyntheticDatasetBuilder;
import org.sharetrace.data.sampling.RiskScoreSampler;
import org.sharetrace.data.sampling.Sampler;
import org.sharetrace.data.sampling.TimeSampler;
import org.sharetrace.graph.Edge;
import org.sharetrace.message.RiskScore;

public abstract class SyntheticExperiment extends Experiment {

  protected final Sampler<RiskScore> riskScoreSampler;
  protected final Sampler<Instant> contactTimeSampler;
  protected int nNodes = -1;

  protected SyntheticExperiment(GraphType graphType, long seed, int nRepeats) {
    super(graphType, nRepeats, seed);
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

  @Override
  protected Dataset newDataset() {
    return SyntheticDatasetBuilder.create()
        .addAllLoggable(loggable())
        .generator(generator())
        .riskScoreFactory(riskScoreFactory())
        .contactTimeFactory(contactTimeFactory())
        .build();
  }

  protected GraphGenerator<Integer, Edge<Integer>, ?> generator() {
    return GraphGeneratorBuilder.<Integer, Edge<Integer>>create(graphType, getNumNodes(), seed)
        .nEdges(getNumNodes() * 2)
        .degree(4)
        .kNearestNeighbors(2)
        .nInitialNodes(2)
        .nNewEdges(2)
        .rewiringProbability(0.3)
        .build();
  }

  private RiskScoreFactory riskScoreFactory() {
    return x -> riskScoreSampler.sample();
  }

  private ContactTimeFactory contactTimeFactory() {
    return (x, xx) -> contactTimeSampler.sample();
  }

  protected int getNumNodes() {
    if (nNodes < 0) {
      throw new IllegalArgumentException("nNodes must be non-negative");
    }
    return nNodes;
  }
}

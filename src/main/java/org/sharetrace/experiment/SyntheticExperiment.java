package org.sharetrace.experiment;

import java.time.Instant;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.factory.ContactTimeFactory;
import org.sharetrace.data.factory.ScoreFactory;
import org.sharetrace.data.factory.SyntheticDatasetBuilder;
import org.sharetrace.data.sampling.Sampler;
import org.sharetrace.data.sampling.ScoreSampler;
import org.sharetrace.data.sampling.TimestampSampler;
import org.sharetrace.graph.Edge;
import org.sharetrace.message.RiskScore;

public abstract class SyntheticExperiment extends Experiment {

  protected final Sampler<RiskScore> scoreSampler;
  protected final Sampler<Instant> contactTimeSampler;
  protected int nNodes = -1;

  protected SyntheticExperiment(GraphType graphType, long seed, int nRepeats) {
    super(graphType, nRepeats, seed);
    this.scoreSampler = newScoreSampler();
    this.contactTimeSampler = newContactTimeSampler();
  }

  protected Sampler<RiskScore> newScoreSampler() {
    return ScoreSampler.builder().timeSampler(newScoreTimeSampler()).seed(seed).build();
  }

  protected Sampler<Instant> newContactTimeSampler() {
    return TimestampSampler.builder()
        .seed(seed)
        .referenceTime(referenceTime)
        .ttl(contactTtl())
        .build();
  }

  protected Sampler<Instant> newScoreTimeSampler() {
    return TimestampSampler.builder()
        .seed(seed)
        .referenceTime(referenceTime)
        .ttl(scoreTtl())
        .build();
  }

  @Override
  protected Dataset newDataset() {
    return SyntheticDatasetBuilder.create()
        .addAllLoggable(loggable())
        .generator(generator())
        .scoreFactory(scoreFactory())
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

  private ScoreFactory scoreFactory() {
    return x -> scoreSampler.sample();
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

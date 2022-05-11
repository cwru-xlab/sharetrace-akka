package org.sharetrace.experiment;

import java.time.Instant;
import java.util.stream.IntStream;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.factory.SyntheticDatasetBuilder;
import org.sharetrace.data.sampling.Sampler;
import org.sharetrace.data.sampling.ScoreSampler;
import org.sharetrace.data.sampling.TimestampSampler;
import org.sharetrace.graph.Edge;
import org.sharetrace.message.Parameters;
import org.sharetrace.message.RiskScore;

public abstract class SyntheticExperiment extends Experiment {

  private static final int NOT_SET = -1;
  protected final GraphType graphType;
  protected final Sampler<RiskScore> scoreSampler;
  protected final Sampler<Instant> contactTimeSampler;
  private final int nRepeats;
  protected int nNodes = NOT_SET;

  protected SyntheticExperiment(GraphType graphType, long seed, int nRepeats) {
    super(seed);
    this.graphType = graphType;
    this.nRepeats = nRepeats;
    this.scoreSampler = newScoreSampler();
    this.contactTimeSampler = newContactTimeSampler();
  }

  protected Sampler<RiskScore> newScoreSampler() {
    return ScoreSampler.builder().timestampSampler(newScoreTimestampSampler()).seed(seed).build();
  }

  protected Sampler<Instant> newContactTimeSampler() {
    return TimestampSampler.builder()
        .seed(seed)
        .referenceTime(referenceTime)
        .ttl(contactTtl())
        .build();
  }

  protected Sampler<Instant> newScoreTimestampSampler() {
    return TimestampSampler.builder()
        .seed(seed)
        .referenceTime(referenceTime)
        .ttl(scoreTtl())
        .build();
  }

  @Override
  public void run() {
    IntStream.range(0, nRepeats).forEach(x -> super.run());
  }

  @Override
  protected Dataset newDataset(Parameters parameters) {
    return SyntheticDatasetBuilder.create()
        .addAllLoggable(loggable())
        .generator(generator())
        .scoreFactory(x -> scoreSampler.sample())
        .contactTimeFactory((x, xx) -> contactTimeSampler.sample())
        .build();
  }

  protected GraphGenerator<Integer, Edge<Integer>, ?> generator() {
    return GraphGeneratorBuilder.<Integer, Edge<Integer>>create(graphType, getNumNodes(), seed)
        .nEdges(getNumNodes() * 2)
        .degree(5)
        .kNearestNeighbors(3)
        .nInitialNodes(2)
        .nNewEdges(2)
        .rewiringProbability(0.5)
        .build();
  }

  protected int getNumNodes() {
    if (nNodes == NOT_SET) {
      throw new IllegalArgumentException("nNodes is not set");
    }
    return nNodes;
  }
}

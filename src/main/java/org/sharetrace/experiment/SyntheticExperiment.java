package org.sharetrace.experiment;

import java.time.Instant;
import java.util.Random;
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

  protected final GraphType graphType;
  protected final Sampler<RiskScore> scoreSampler;
  protected final Sampler<Instant> contactTimeSampler;
  protected int nNodes;
  protected int nEdges;

  protected SyntheticExperiment(GraphType graphType, int nNodes, int nEdges, long seed) {
    super(seed);
    this.graphType = graphType;
    this.nNodes = nNodes;
    this.nEdges = nEdges;
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
  protected Dataset newDataset(Parameters parameters) {
    return SyntheticDatasetBuilder.create()
        .addAllLoggable(loggable())
        .generator(newGenerator())
        .scoreFactory(x -> scoreSampler.sample())
        .contactTimeFactory((x, xx) -> contactTimeSampler.sample())
        .build();
  }

  protected GraphGenerator<Integer, Edge<Integer>, ?> newGenerator() {
    return GraphGeneratorBuilder.<Integer, Edge<Integer>>create(graphType)
        .nNodes(nNodes)
        .nEdges(nEdges)
        .random(new Random(seed))
        .build();
  }
}

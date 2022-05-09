package org.sharetrace.data;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.math3.distribution.RealDistribution;
import org.immutables.builder.Builder;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.graph.Edge;
import org.sharetrace.logging.Loggable;
import org.sharetrace.message.RiskScore;

class SyntheticDatasetFactory extends DatasetFactory {

  private final GraphGenerator<Integer, Edge<Integer>, ?> generator;
  private final Supplier<Instant> clock;
  private final long scoreTtlSeconds;
  private final long contactTtlSeconds;
  private final RealDistribution scoreValueDistribution;
  private final RealDistribution lookBackDistribution;

  private SyntheticDatasetFactory(
      Set<Class<? extends Loggable>> loggable,
      GraphGenerator<Integer, Edge<Integer>, ?> generator,
      Supplier<Instant> clock,
      Duration scoreTtl,
      Duration contactTtl,
      RealDistribution scoreValueDistribution,
      RealDistribution lookBackDistribution) {
    super(loggable);
    this.generator = generator;
    this.clock = clock;
    this.scoreTtlSeconds = scoreTtl.toSeconds();
    this.contactTtlSeconds = contactTtl.toSeconds();
    this.scoreValueDistribution = scoreValueDistribution;
    this.lookBackDistribution = lookBackDistribution;
  }

  @Builder.Factory
  protected static Dataset<Integer> syntheticDataset(
      Set<Class<? extends Loggable>> loggable,
      GraphGenerator<Integer, Edge<Integer>, ?> generator,
      Supplier<Instant> clock,
      Duration scoreTtl,
      Duration contactTtl,
      RealDistribution scoreValueDistribution,
      RealDistribution lookBackDistribution) {
    return new SyntheticDatasetFactory(
            loggable,
            generator,
            clock,
            scoreTtl,
            contactTtl,
            scoreValueDistribution,
            lookBackDistribution)
        .createDataset();
  }

  @Override
  public void createTemporalGraph(Graph<Integer, Edge<Integer>> target) {
    generator.generateGraph(target);
  }

  @Override
  protected RiskScore scoreOf(int node) {
    return RiskScore.builder()
        .value(scoreValueDistribution.sample())
        .timestamp(randomTimestamp(scoreTtlSeconds))
        .build();
  }

  @Override
  protected Instant contactedAt(int node1, int node2) {
    return randomTimestamp(contactTtlSeconds);
  }

  private Instant randomTimestamp(long ttlSeconds) {
    Duration lookBack = Duration.ofSeconds(Math.round(normalizedLookBackSample() * ttlSeconds));
    return clock.get().minus(lookBack);
  }

  public double normalizedLookBackSample() {
    double max = lookBackDistribution.getSupportUpperBound();
    double min = lookBackDistribution.getSupportLowerBound();
    return (lookBackDistribution.sample() - min) / (max - min);
  }
}

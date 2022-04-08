package org.sharetrace.data;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Supplier;
import org.immutables.builder.Builder;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.model.graph.Edge;
import org.sharetrace.model.message.RiskScore;

class SyntheticDatasetFactory extends DatasetFactory {

  private final GraphGenerator<Integer, Edge<Integer>, ?> generator;
  private final Supplier<Instant> clock;
  private final long scoreTtlInSeconds;

  private SyntheticDatasetFactory(
      GraphGenerator<Integer, Edge<Integer>, ?> generator,
      Supplier<Instant> clock,
      Duration scoreTtl) {
    this.generator = generator;
    this.clock = clock;
    this.scoreTtlInSeconds = scoreTtl.toSeconds();
  }

  @Builder.Factory
  protected static Dataset<Integer> syntheticDataset(
      GraphGenerator<Integer, Edge<Integer>, ?> generator,
      Supplier<Instant> clock,
      Duration scoreTtl) {
    return new SyntheticDatasetFactory(generator, clock, scoreTtl).createDataset();
  }

  @Override
  public void generateGraph(Graph<Integer, Edge<Integer>> target, Map<String, Integer> resultMap) {
    generator.generateGraph(target);
  }

  @Override
  protected RiskScore scoreOf(int node) {
    return RiskScore.builder().value(Math.random()).timestamp(randomTimestamp()).build();
  }

  @Override
  protected Instant contactedAt(int node1, int node2) {
    return randomTimestamp();
  }

  private Instant randomTimestamp() {
    Duration lookBack = Duration.ofSeconds(Math.round(Math.random() * scoreTtlInSeconds));
    return clock.get().minus(lookBack);
  }
}

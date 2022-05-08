package org.sharetrace.data;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
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
  private final Random random;

  private SyntheticDatasetFactory(
      Set<Class<? extends Loggable>> loggable,
      GraphGenerator<Integer, Edge<Integer>, ?> generator,
      Supplier<Instant> clock,
      Duration scoreTtl,
      Duration contactTtl,
      Random random) {
    super(loggable);
    this.generator = generator;
    this.clock = clock;
    this.scoreTtlSeconds = scoreTtl.toSeconds();
    this.contactTtlSeconds = contactTtl.toSeconds();
    this.random = random;
  }

  @Builder.Factory
  protected static Dataset<Integer> syntheticDataset(
      Set<Class<? extends Loggable>> loggable,
      GraphGenerator<Integer, Edge<Integer>, ?> generator,
      Supplier<Instant> clock,
      Duration scoreTtl,
      Duration contactTtl,
      Random random) {
    return new SyntheticDatasetFactory(loggable, generator, clock, scoreTtl, contactTtl, random)
        .createDataset();
  }

  @Override
  public void createTemporalGraph(Graph<Integer, Edge<Integer>> target) {
    generator.generateGraph(target);
  }

  @Override
  protected RiskScore scoreOf(int node) {
    return RiskScore.builder()
        .value(random.nextDouble())
        .timestamp(randomTimestamp(scoreTtlSeconds))
        .build();
  }

  @Override
  protected Instant contactedAt(int node1, int node2) {
    return randomTimestamp(contactTtlSeconds);
  }

  private Instant randomTimestamp(long ttlSeconds) {
    Duration lookBack = Duration.ofSeconds(Math.round(random.nextDouble() * ttlSeconds));
    return clock.get().minus(lookBack);
  }
}

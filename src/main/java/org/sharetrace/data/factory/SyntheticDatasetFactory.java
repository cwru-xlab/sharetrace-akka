package org.sharetrace.data.factory;

import java.time.Instant;
import java.util.Set;
import org.immutables.builder.Builder;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.data.Dataset;
import org.sharetrace.graph.Edge;
import org.sharetrace.logging.Loggable;
import org.sharetrace.message.RiskScore;

class SyntheticDatasetFactory extends DatasetFactory {

  private final Set<Class<? extends Loggable>> loggable;
  private final RiskScoreFactory riskScoreFactory;
  private final ContactTimeFactory contactTimeFactory;
  private final GraphGenerator<Integer, Edge<Integer>, ?> generator;

  private SyntheticDatasetFactory(
      Set<Class<? extends Loggable>> loggable,
      RiskScoreFactory riskScoreFactory,
      ContactTimeFactory contactTimeFactory,
      GraphGenerator<Integer, Edge<Integer>, ?> generator) {
    this.loggable = loggable;
    this.riskScoreFactory = riskScoreFactory;
    this.contactTimeFactory = contactTimeFactory;
    this.generator = generator;
  }

  @Builder.Factory
  public static Dataset syntheticDataset(
      Set<Class<? extends Loggable>> loggable,
      RiskScoreFactory riskScoreFactory,
      ContactTimeFactory contactTimeFactory,
      GraphGenerator<Integer, Edge<Integer>, ?> generator) {
    return new SyntheticDatasetFactory(loggable, riskScoreFactory, contactTimeFactory, generator)
        .newDataset();
  }

  @Override
  public void newContactNetwork(Graph<Integer, Edge<Integer>> target) {
    generator.generateGraph(target);
  }

  @Override
  protected Set<Class<? extends Loggable>> loggable() {
    return loggable;
  }

  @Override
  public Instant getContactTime(int user1, int user2) {
    return contactTimeFactory.getContactTime(user1, user2);
  }

  @Override
  public RiskScore getRiskScore(int user) {
    return riskScoreFactory.getRiskScore(user);
  }
}

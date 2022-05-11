package org.sharetrace.data.factory;

import java.util.Set;
import org.immutables.builder.Builder;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.data.Dataset;
import org.sharetrace.graph.Edge;
import org.sharetrace.logging.Loggable;

class SyntheticDatasetFactory extends DatasetFactory {

  private final Set<Class<? extends Loggable>> loggable;
  private final ScoreFactory scoreFactory;
  private final ContactTimeFactory contactTimeFactory;
  private final GraphGenerator<Integer, Edge<Integer>, ?> generator;

  private SyntheticDatasetFactory(
      Set<Class<? extends Loggable>> loggable,
      ScoreFactory scoreFactory,
      ContactTimeFactory contactTimeFactory,
      GraphGenerator<Integer, Edge<Integer>, ?> generator) {
    this.loggable = loggable;
    this.scoreFactory = scoreFactory;
    this.contactTimeFactory = contactTimeFactory;
    this.generator = generator;
  }

  @Builder.Factory
  public static Dataset syntheticDataset(
      Set<Class<? extends Loggable>> loggable,
      ScoreFactory scoreFactory,
      ContactTimeFactory contactTimeFactory,
      GraphGenerator<Integer, Edge<Integer>, ?> generator) {
    return new SyntheticDatasetFactory(loggable, scoreFactory, contactTimeFactory, generator)
        .create();
  }

  @Override
  public void createTemporalGraph(Graph<Integer, Edge<Integer>> target) {
    generator.generateGraph(target);
  }

  @Override
  protected Set<Class<? extends Loggable>> loggable() {
    return loggable;
  }

  @Override
  protected ScoreFactory scoreFactory() {
    return scoreFactory;
  }

  @Override
  protected ContactTimeFactory contactTimeFactory() {
    return contactTimeFactory;
  }
}

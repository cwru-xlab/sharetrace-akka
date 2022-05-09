package org.sharetrace.data;

import java.util.Set;
import org.immutables.builder.Builder;
import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.graph.Edge;
import org.sharetrace.logging.Loggable;

class SyntheticDatasetFactory extends DatasetFactory {

  private final GraphGenerator<Integer, Edge<Integer>, ?> generator;

  private SyntheticDatasetFactory(
      Set<Class<? extends Loggable>> loggable,
      ScoreFactory scoreFactory,
      ContactTimeFactory contactTimeFactory,
      GraphGenerator<Integer, Edge<Integer>, ?> generator) {
    super(loggable, scoreFactory, contactTimeFactory);
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
}

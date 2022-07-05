package org.sharetrace.experiment;

import java.util.Objects;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.data.SyntheticDataset;
import org.sharetrace.data.factory.GraphGeneratorBuilder;
import org.sharetrace.data.factory.GraphGeneratorFactory;
import org.sharetrace.graph.Edge;

public class SyntheticExperiment extends Experiment {

  private final GraphGeneratorFactory graphGeneratorFactory;
  protected int nNodes = -1;

  protected SyntheticExperiment(Builder builder) {
    super(builder);
    this.graphGeneratorFactory = builder.generatorFactory;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  protected void setDataset() {
    dataset =
        SyntheticDataset.builder()
            .addAllLoggable(loggable())
            .graphGenerator(getGraphGenerator())
            .riskScoreFactory(riskScoreFactory())
            .contactTimeFactory(contactTimeFactory())
            .build();
  }

  protected GraphGenerator<Integer, Edge<Integer>, ?> getGraphGenerator() {
    return graphGeneratorFactory.getGraphGenerator(nNodes);
  }

  public static class Builder extends Experiment.Builder {

    protected GraphGeneratorFactory generatorFactory;

    public Builder graphGeneratorFactory(GraphGeneratorFactory generatorFactory) {
      this.generatorFactory = generatorFactory;
      return this;
    }

    @Override
    public void preBuild() {
      generatorFactory = Objects.requireNonNullElseGet(generatorFactory, this::defaultFactory);
      super.preBuild();
    }

    @Override
    public Experiment build() {
      preBuild();
      return new SyntheticExperiment(this);
    }

    protected GraphGeneratorFactory defaultFactory() {
      return nNodes ->
          GraphGeneratorBuilder.<Integer, Edge<Integer>>create(graphType, nNodes, seed)
              .nEdges(nNodes * 2)
              .degree(4)
              .kNearestNeighbors(2)
              .nInitialNodes(2)
              .nNewEdges(2)
              .rewiringProbability(0.3)
              .build();
    }
  }
}

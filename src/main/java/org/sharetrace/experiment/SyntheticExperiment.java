package org.sharetrace.experiment;

import java.util.Random;
import org.jgrapht.generate.GraphGenerator;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.factory.SyntheticDatasetBuilder;
import org.sharetrace.graph.Edge;
import org.sharetrace.message.Parameters;

public abstract class SyntheticExperiment extends Experiment {

  protected final GraphType graphType;
  protected int nNodes;
  protected int nEdges;

  protected SyntheticExperiment(GraphType graphType, int nNodes, int nEdges, long seed) {
    super(seed);
    this.graphType = graphType;
    this.nNodes = nNodes;
    this.nEdges = nEdges;
  }

  @Override
  protected Dataset newDataset(Parameters parameters) {
    return SyntheticDatasetBuilder.create()
        .addAllLoggable(loggable())
        .generator(newGenerator())
        .scoreFactory(scoreFactory())
        .contactTimeFactory(contactTimeFactory())
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

package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import java.time.Duration;
import java.util.Random;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.data.SyntheticDatasetBuilder;
import org.sharetrace.model.message.AlgorithmMessage;
import org.sharetrace.model.message.Parameters;

public class RuntimeExperiment extends Experiment<Integer> {

  private final GraphType graphType;
  private final int minNodes;
  private final int maxNodes;
  private final int stepNodes;
  private final int nRepeats;
  private final long seed;

  public RuntimeExperiment(
      GraphType graphType, int minNodes, int maxNodes, int stepNodes, int nRepeats, long seed) {
    this.graphType = graphType;
    this.minNodes = minNodes;
    this.maxNodes = maxNodes;
    this.stepNodes = stepNodes;
    this.nRepeats = nRepeats;
    this.seed = seed;
  }

  @Override
  public void run() {
    Parameters parameters = parameters();
    Dataset<Integer> dataset;
    Behavior<AlgorithmMessage> algorithm;
    for (int nNodes = minNodes; nNodes < maxNodes; nNodes += stepNodes) {
      for (int iRepeat = 0; iRepeat < nRepeats; iRepeat++) {
        dataset = newDataset(parameters);
        algorithm = newAlgorithm(dataset, parameters);
        Runner.run(algorithm);
      }
    }
  }

  @Override
  protected Dataset<Integer> newDataset(Parameters parameters) {
    return SyntheticDatasetBuilder.create()
        .generator(GraphGeneratorFactory.create(graphType, new Random(seed)))
        .clock(clock())
        .scoreTtl(parameters.scoreTtl())
        .contactTtl(parameters.contactTtl())
        .random(new Random(seed))
        .build();
  }

  @Override
  protected Behavior<AlgorithmMessage> newAlgorithm(
      Dataset<Integer> dataset, Parameters parameters) {
    return RiskPropagationBuilder.<Integer>create()
        .graph(dataset.graph())
        .parameters(parameters)
        .clock(clock())
        .scoreFactory(dataset::scoreOf)
        .timeFactory(dataset::contactedAt)
        .cacheFactory(cacheFactory())
        .nodeTimeout(Duration.ofSeconds(5L)) // TODO Scale based on graph size
        .nodeRefreshRate(Duration.ofHours(1L))
        .build();
  }
}

package org.sharetrace.experiment;

import akka.actor.typed.Behavior;
import java.time.Duration;
import org.sharetrace.RiskPropagationBuilder;
import org.sharetrace.Runner;
import org.sharetrace.data.Dataset;
import org.sharetrace.model.message.AlgorithmMessage;
import org.sharetrace.model.message.Parameters;

public class RuntimeExperiment extends SyntheticExperiment {

  private final int minNodes;
  private final int maxNodes;
  private final int stepNodes;
  private final int nRepeats;

  public RuntimeExperiment(
      GraphType graphType, int minNodes, int maxNodes, int stepNodes, int nRepeats, long seed) {
    super(graphType, minNodes, minNodes, seed);
    this.minNodes = minNodes;
    this.maxNodes = maxNodes;
    this.stepNodes = stepNodes;
    this.nRepeats = nRepeats;
  }

  @Override
  public void run() {
    Parameters parameters = parameters();
    Dataset<Integer> dataset;
    Behavior<AlgorithmMessage> algorithm;
    for (int iNodes = minNodes; iNodes < maxNodes; iNodes += stepNodes) {
      nNodes = iNodes;
      nEdges = iNodes * 2;
      for (int iRepeat = 0; iRepeat < nRepeats; iRepeat++) {
        dataset = newDataset(parameters);
        algorithm = newAlgorithm(dataset, parameters);
        Runner.run(algorithm);
      }
    }
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

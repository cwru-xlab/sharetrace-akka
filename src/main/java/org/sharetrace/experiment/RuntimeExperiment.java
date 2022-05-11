package org.sharetrace.experiment;

import java.time.Duration;
import java.util.Random;
import java.util.Set;
import org.sharetrace.data.Dataset;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.metrics.GraphCycleMetrics;
import org.sharetrace.logging.metrics.GraphSizeMetrics;
import org.sharetrace.logging.metrics.RuntimeMetric;
import org.sharetrace.message.Parameters;

public final class RuntimeExperiment extends SyntheticExperiment {

  private final int minNodes;
  private final int maxNodes;
  private final int stepNodes;
  private Duration nodeTimeout;

  public RuntimeExperiment(GraphType graphType, int nNodes, long seed) {
    this(graphType, nNodes, nNodes, 1, 1, seed);
  }

  public RuntimeExperiment(
      GraphType graphType, int minNodes, int maxNodes, int stepNodes, int nRepeats, long seed) {
    super(graphType, seed, nRepeats);
    this.minNodes = minNodes;
    this.maxNodes = maxNodes;
    this.stepNodes = stepNodes;
  }

  public RuntimeExperiment(GraphType graphType, int nNodes) {
    this(graphType, nNodes, nNodes, 1, 1, new Random().nextLong());
  }

  @Override
  public void run() {
    for (nNodes = minNodes; nNodes <= maxNodes; nNodes += stepNodes) {
      super.run();
    }
  }

  @Override
  protected Dataset newDataset(Parameters parameters) {
    Dataset dataset = super.newDataset(parameters);
    nodeTimeout = computeNodeTimeout(dataset.graph());
    return dataset;
  }

  @Override
  protected Set<Class<? extends Loggable>> loggable() {
    // Events and path-based graph metrics becomes too expensive for large graphs
    return Set.of(GraphCycleMetrics.class, GraphSizeMetrics.class, RuntimeMetric.class);
  }

  @Override
  protected Duration nodeTimeout() {
    return nodeTimeout;
  }
}

package org.sharetrace.experiment;

import java.util.Random;
import java.util.Set;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.metrics.GraphSizeMetrics;
import org.sharetrace.logging.metrics.RuntimeMetric;

public final class RuntimeExperiment extends SyntheticExperiment {

  private final int minNodes;
  private final int maxNodes;
  private final int stepNodes;

  public RuntimeExperiment(
      GraphType graphType, int minNodes, int maxNodes, int stepNodes, int nRepeats, long seed) {
    super(graphType, seed, nRepeats);
    this.minNodes = minNodes;
    this.maxNodes = maxNodes;
    this.stepNodes = stepNodes;
  }

  public static void run(GraphType graphType, int nNodes) {
    run(graphType, nNodes, new Random().nextLong());
  }

  public static void run(GraphType graphType, int nNodes, long seed) {
    run(graphType, nNodes, nNodes, 1, 1, seed);
  }

  public static void run(
      GraphType graphType, int minNodes, int maxNodes, int stepNodes, int nRepeats, long seed) {
    new RuntimeExperiment(graphType, minNodes, maxNodes, stepNodes, nRepeats, seed).run();
  }

  @Override
  protected Set<Class<? extends Loggable>> loggable() {
    // Events and path-based graph metrics becomes too expensive for large graphs
    return Set.of(GraphSizeMetrics.class, RuntimeMetric.class);
  }

  @Override
  public void run() {
    for (nNodes = minNodes; nNodes <= maxNodes; nNodes += stepNodes) {
      super.run();
    }
  }
}

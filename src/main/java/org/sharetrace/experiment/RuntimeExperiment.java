package org.sharetrace.experiment;

import java.time.Duration;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.metrics.GraphCycleMetrics;
import org.sharetrace.logging.metrics.GraphSizeMetrics;
import org.sharetrace.logging.metrics.RuntimeMetric;

public final class RuntimeExperiment extends SyntheticExperiment {

  private final int minNodes;
  private final int maxNodes;
  private final int stepNodes;
  private final int nRepeats;

  public RuntimeExperiment(GraphType graphType, int nNodes, long seed) {
    this(graphType, nNodes, nNodes, 1, 1, seed);
  }

  public RuntimeExperiment(
      GraphType graphType, int minNodes, int maxNodes, int stepNodes, int nRepeats, long seed) {
    super(graphType, minNodes, minNodes, seed);
    this.minNodes = minNodes;
    this.maxNodes = maxNodes;
    this.stepNodes = stepNodes;
    this.nRepeats = nRepeats;
  }

  public RuntimeExperiment(GraphType graphType, int nNodes) {
    this(graphType, nNodes, nNodes, 1, 1, new Random().nextLong());
  }

  @Override
  public void run() {
    for (int n = minNodes; n <= maxNodes; n += stepNodes) {
      nNodes = n;
      nEdges = n * 2;
      IntStream.range(0, nRepeats).forEach(x -> super.run());
    }
  }

  @Override
  protected Set<Class<? extends Loggable>> loggable() {
    // Events and path-based graph metrics becomes too expensive for large graphs
    return Set.of(GraphCycleMetrics.class, GraphSizeMetrics.class, RuntimeMetric.class);
  }

  @Override
  protected Duration nodeTimeout() {
    return Duration.ofSeconds((long) Math.ceil(Math.log(nEdges)));
  }
}

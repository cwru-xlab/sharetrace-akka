package org.sharetrace.experiment;

import java.util.Set;
import java.util.stream.IntStream;
import org.sharetrace.logging.Loggable;
import org.sharetrace.logging.metrics.RuntimeMetric;

public class RuntimeExperiment extends SyntheticExperiment {

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
    // Logging events and graph metrics becomes too expensive for large graphs
    return Set.of(RuntimeMetric.class);
  }
}

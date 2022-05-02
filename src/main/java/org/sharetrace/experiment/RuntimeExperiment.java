package org.sharetrace.experiment;

import java.util.stream.IntStream;
import org.sharetrace.util.logging.EventLog;
import org.sharetrace.util.logging.NodeEvent;

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
    for (int n = minNodes; n < maxNodes; n += stepNodes) {
      nNodes = n;
      nEdges = n * 2;
      IntStream.range(0, nRepeats).forEach(x -> super.run());
    }
  }

  @Override
  protected EventLog<NodeEvent> log() {
    return EventLog.disableAll(NodeEvent.class);
  }
}

package org.sharetrace.data;

import java.time.Instant;
import org.sharetrace.graph.TemporalGraph;
import org.sharetrace.message.RiskScore;

public interface Dataset {

  RiskScore getScore(int node);

  Instant getContactTime(int node1, int node2);

  TemporalGraph graph();
}

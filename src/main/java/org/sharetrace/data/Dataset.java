package org.sharetrace.data;

import java.time.Instant;
import org.sharetrace.model.graph.TemporalGraph;
import org.sharetrace.model.message.RiskScore;

public interface Dataset<T> {

  RiskScore score(T node);

  Instant timestamp(T node1, T node2);

  TemporalGraph<T> graph();
}

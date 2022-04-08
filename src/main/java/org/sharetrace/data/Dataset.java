package org.sharetrace.data;

import java.time.Instant;
import org.sharetrace.model.graph.TemporalGraph;
import org.sharetrace.model.message.RiskScore;

public interface Dataset<T> {

  RiskScore scoreOf(T node);

  Instant contactedAt(T node1, T node2);

  TemporalGraph<T> graph();
}

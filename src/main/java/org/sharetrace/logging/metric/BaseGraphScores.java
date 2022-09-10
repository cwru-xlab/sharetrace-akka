package org.sharetrace.logging.metric;

import org.immutables.value.Value;
import org.sharetrace.util.Stats;

@Value.Immutable
interface BaseGraphScores extends LoggableMetric {

  Stats harmonicCentrality();

  Stats katzCentrality();

  Stats eigenvectorCentrality();

  float degeneracy();

  float globalClusteringCoefficient();

  Stats localClusteringCoefficient();
}

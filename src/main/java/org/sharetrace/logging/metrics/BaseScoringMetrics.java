package org.sharetrace.logging.metrics;

import org.immutables.value.Value;
import org.sharetrace.util.Stats;

@Value.Immutable
interface BaseScoringMetrics extends LoggableMetric {

  Stats harmonicCentrality();

  Stats katzCentrality();

  Stats eigenvectorCentrality();

  float degeneracy();

  float globalClusteringCoefficient();

  Stats localClusteringCoefficient();
}

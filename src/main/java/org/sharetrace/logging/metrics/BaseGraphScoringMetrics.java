package org.sharetrace.logging.metrics;

import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphScoringMetrics extends LoggableMetric {

  double harmonicCentrality();

  double katzCentrality();

  double eigenvectorCentrality();

  double degeneracy();

  double globalClusteringCoefficient();

  double localClusteringCoefficient();
}

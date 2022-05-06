package org.sharetrace.logging.metrics;

import org.immutables.value.Value;
import org.sharetrace.util.DescriptiveStats;

@Value.Immutable
interface BaseGraphScoringMetrics extends LoggableMetric {

  DescriptiveStats harmonicCentrality();

  DescriptiveStats katzCentrality();

  DescriptiveStats eigenvectorCentrality();

  double degeneracy();

  double globalClusteringCoefficient();

  DescriptiveStats localClusteringCoefficient();
}

package org.sharetrace.logging.metrics;

import org.immutables.value.Value;
import org.sharetrace.util.DescriptiveStats;

@Value.Immutable
interface BaseScoringMetrics extends LoggableMetric {

  DescriptiveStats harmonicCentrality();

  DescriptiveStats katzCentrality();

  DescriptiveStats eigenvectorCentrality();

  float degeneracy();

  float globalClusteringCoefficient();

  DescriptiveStats localClusteringCoefficient();
}

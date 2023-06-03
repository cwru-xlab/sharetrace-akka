package io.sharetrace.util.logging.metric;

import io.sharetrace.util.Statistics;
import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphScores extends MetricRecord {

  Statistics harmonicCentrality();

  Statistics katzCentrality();

  Statistics eigenvectorCentrality();

  double degeneracy();

  double globalClusteringCoefficient();

  Statistics localClusteringCoefficient();
}

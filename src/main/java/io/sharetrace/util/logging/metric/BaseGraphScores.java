package io.sharetrace.util.logging.metric;

import io.sharetrace.util.Stats;
import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphScores extends LoggableMetric {

  Stats harmonicCentrality();

  Stats katzCentrality();

  Stats eigenvectorCentrality();

  double degeneracy();

  double globalClusteringCoefficient();

  Stats localClusteringCoefficient();
}

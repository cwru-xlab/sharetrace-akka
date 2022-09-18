package io.sharetrace.logging.metric;

import io.sharetrace.util.Stats;
import org.immutables.value.Value;

@Value.Immutable
interface BaseGraphScores extends LoggableMetric {

  Stats harmonicCentrality();

  Stats katzCentrality();

  Stats eigenvectorCentrality();

  float degeneracy();

  float globalClusteringCoefficient();

  Stats localClusteringCoefficient();
}

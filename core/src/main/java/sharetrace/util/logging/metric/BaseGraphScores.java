package sharetrace.util.logging.metric;

import org.immutables.value.Value;
import sharetrace.util.Statistics;

@Value.Immutable
interface BaseGraphScores extends MetricRecord {

  Statistics harmonicCentrality();

  Statistics katzCentrality();

  Statistics eigenvectorCentrality();

  double degeneracy();

  double globalClusteringCoefficient();

  Statistics localClusteringCoefficient();
}

package sharetrace.graph;

import java.util.function.Supplier;
import sharetrace.util.logging.Logging;
import sharetrace.util.logging.RecordLogger;
import sharetrace.util.logging.metric.GraphCycles;
import sharetrace.util.logging.metric.GraphEccentricity;
import sharetrace.util.logging.metric.GraphScores;
import sharetrace.util.logging.metric.GraphSize;
import sharetrace.util.logging.metric.GraphTopology;
import sharetrace.util.logging.metric.MetricRecord;

public final class TemporalNetworkLogger {

  private static final RecordLogger<MetricRecord> LOGGER = Logging.metricsLogger();

  private TemporalNetworkLogger() {}

  public static void logMetrics(TemporalNetwork<?> network) {
    GraphStatistics<?, ?> statistics = GraphStatistics.of(network);
    logMetric(GraphSize.class, statistics::graphSize);
    logMetric(GraphCycles.class, statistics::graphCycles);
    logMetric(GraphEccentricity.class, statistics::graphEccentricity);
    logMetric(GraphScores.class, statistics::graphScores);
    if (logMetric(GraphTopology.class, graphTopology(network))) {
      Exporter.export(network, network.id());
    }
  }

  private static Supplier<GraphTopology> graphTopology(TemporalNetwork<?> network) {
    return () -> GraphTopology.of(network.id());
  }

  private static <T extends MetricRecord> boolean logMetric(Class<T> type, Supplier<T> metric) {
    return LOGGER.log(MetricRecord.KEY, type, metric);
  }
}

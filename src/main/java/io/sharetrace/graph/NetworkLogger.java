package io.sharetrace.graph;

import io.sharetrace.util.logging.Logging;
import io.sharetrace.util.logging.RecordLogger;
import io.sharetrace.util.logging.metric.GraphCycles;
import io.sharetrace.util.logging.metric.GraphEccentricity;
import io.sharetrace.util.logging.metric.GraphScores;
import io.sharetrace.util.logging.metric.GraphSize;
import io.sharetrace.util.logging.metric.GraphTopology;
import io.sharetrace.util.logging.metric.MetricRecord;
import java.util.function.Supplier;

public final class NetworkLogger {

  private static final RecordLogger<MetricRecord> LOGGER = Logging.metricsLogger();

  private NetworkLogger() {}

  public static void logMetrics(TemporalNetwork<?> network) {
    GraphStatistics<?, ?> statistics = GraphStatistics.of(network);
    logMetric(GraphSize.class, statistics::graphSize);
    logMetric(GraphCycles.class, statistics::graphCycles);
    logMetric(GraphEccentricity.class, statistics::graphEccentricity);
    logMetric(GraphScores.class, statistics::graphScores);
    if (logMetric(GraphTopology.class, graphTopology(network))) {
      Exporter.export(network, "network-" + network.id());
    }
  }

  private static Supplier<GraphTopology> graphTopology(TemporalNetwork<?> network) {
    return () -> GraphTopology.of(network.id());
  }

  private static <T extends MetricRecord> boolean logMetric(Class<T> type, Supplier<T> metric) {
    return LOGGER.log(MetricRecord.KEY, type, metric);
  }
}

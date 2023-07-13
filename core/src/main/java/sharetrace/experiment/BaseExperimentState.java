package sharetrace.experiment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.immutables.value.Value;
import sharetrace.actor.RiskPropagationBuilder;
import sharetrace.experiment.data.RiskScoreFactory;
import sharetrace.graph.Exporter;
import sharetrace.graph.GraphStatistics;
import sharetrace.graph.TemporalNetwork;
import sharetrace.graph.TemporalNetworkFactory;
import sharetrace.model.Identifiable;
import sharetrace.model.Parameters;
import sharetrace.util.IdFactory;
import sharetrace.util.logging.LogRecord;
import sharetrace.util.logging.Logging;
import sharetrace.util.logging.RecordLogger;
import sharetrace.util.logging.metric.GraphCycles;
import sharetrace.util.logging.metric.GraphEccentricity;
import sharetrace.util.logging.metric.GraphScores;
import sharetrace.util.logging.metric.GraphSize;
import sharetrace.util.logging.metric.GraphTopology;
import sharetrace.util.logging.metric.MetricRecord;
import sharetrace.util.logging.setting.ExperimentSettings;
import sharetrace.util.logging.setting.SettingsRecord;

@Value.Immutable
abstract class BaseExperimentState<K> implements Runnable, Identifiable {

  private static final RecordLogger<SettingsRecord> SETTINGS_LOGGER = Logging.settingsLogger();
  private static final RecordLogger<MetricRecord> METRICS_LOGGER = Logging.metricsLogger();

  public abstract Parameters parameters();

  @JsonIgnore
  public abstract TemporalNetworkFactory<K> networkFactory();

  @JsonIgnore
  public abstract RiskScoreFactory<K> scoreFactory();

  public abstract Set<Class<? extends LogRecord>> loggable();

  public abstract Context context();

  @Value.Default
  public TemporalNetwork<K> contactNetwork() {
    return networkFactory().getNetwork();
  }

  @Value.Derived
  public String id() {
    return IdFactory.newIntString();
  }

  @JsonIgnore
  public ExperimentState<K> withNewNetwork() {
    return ExperimentState.copyOf(this).withContactNetwork(contactNetwork());
  }

  public void run(int iterations) {
    IntStream.range(0, iterations).forEach(x -> ExperimentState.copyOf(this).run());
  }

  @Override
  public void run() {
    setUpLogging();
    logMetricsAndSettings();
    runAlgorithm();
  }

  private void setUpLogging() {
    Logging.setMdc(id());
    Logging.enable(loggable());
  }

  private void logMetricsAndSettings() {
    logMetrics();
    SETTINGS_LOGGER.log(SettingsRecord.KEY, ExperimentSettings.class, this::settings);
  }

  private void runAlgorithm() {
    RiskPropagationBuilder.<K>create()
        .parameters(parameters())
        .scoreFactory(scoreFactory())
        .contactNetwork(contactNetwork())
        .clock(context().clock())
        .build()
        .run();
  }

  private ExperimentSettings settings() {
    return ExperimentSettings.builder()
        .state(id())
        .context(context())
        .parameters(parameters())
        .networkType(contactNetwork().type())
        .network(contactNetwork().id())
        .build();
  }

  public void logMetrics() {
    GraphStatistics<?, ?> statistics = GraphStatistics.of(contactNetwork());
    logMetric(GraphSize.class, statistics::graphSize);
    logMetric(GraphCycles.class, statistics::graphCycles);
    logMetric(GraphEccentricity.class, statistics::graphEccentricity);
    logMetric(GraphScores.class, statistics::graphScores);
    if (logMetric(GraphTopology.class, graphTopology(contactNetwork()))) {
      Exporter.export(contactNetwork(), contactNetwork().id());
    }
  }

  private static Supplier<GraphTopology> graphTopology(TemporalNetwork<?> network) {
    return () -> GraphTopology.of(network.id());
  }

  private static <T extends MetricRecord> boolean logMetric(Class<T> type, Supplier<T> metric) {
    return METRICS_LOGGER.log(MetricRecord.KEY, type, metric);
  }
}

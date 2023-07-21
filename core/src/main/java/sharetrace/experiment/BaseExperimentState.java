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
    logMetrics();
    logSettings();
    runAlgorithm();
  }

  private void setUpLogging() {
    Logging.setMdc(id());
    Logging.enable(loggable());
  }

  private void logMetrics() {
    GraphStatistics<?, ?> statistics = GraphStatistics.of(contactNetwork());
    METRICS_LOGGER.log(GraphSize.class, statistics::graphSize);
    METRICS_LOGGER.log(GraphCycles.class, statistics::graphCycles);
    METRICS_LOGGER.log(GraphEccentricity.class, statistics::graphEccentricity);
    METRICS_LOGGER.log(GraphScores.class, statistics::graphScores);
    Supplier<GraphTopology> graphTopology = () -> GraphTopology.of(contactNetwork().id());
    if (METRICS_LOGGER.log(GraphTopology.class, graphTopology)) {
      Exporter.export(contactNetwork(), contactNetwork().id());
    }
  }

  private void logSettings() {
    SETTINGS_LOGGER.log(ExperimentSettings.class, this::settings);
  }

  private ExperimentSettings settings() {
    return ExperimentSettings.builder()
        .stateId(id())
        .context(context())
        .parameters(parameters())
        .networkType(contactNetwork().type())
        .networkId(contactNetwork().id())
        .build();
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
}

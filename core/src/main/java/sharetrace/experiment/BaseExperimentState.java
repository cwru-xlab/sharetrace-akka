package sharetrace.experiment;

import java.util.Set;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;
import sharetrace.actor.RiskPropagationBuilder;
import sharetrace.experiment.data.RiskScoreFactory;
import sharetrace.graph.TemporalNetwork;
import sharetrace.graph.TemporalNetworkFactory;
import sharetrace.graph.TemporalNetworkLogger;
import sharetrace.model.Identifiable;
import sharetrace.model.UserParameters;
import sharetrace.model.message.RiskScoreMessage;
import sharetrace.util.Identifiers;
import sharetrace.util.cache.CacheParameters;
import sharetrace.util.cache.IntervalCache;
import sharetrace.util.logging.LogRecord;
import sharetrace.util.logging.Logging;
import sharetrace.util.logging.RecordLogger;
import sharetrace.util.logging.setting.ExperimentSettings;
import sharetrace.util.logging.setting.SettingsRecord;

@Value.Immutable
abstract class BaseExperimentState<K> implements Runnable, Identifiable {

  private static final RecordLogger<SettingsRecord> LOGGER = Logging.settingsLogger();

  public abstract CacheParameters<RiskScoreMessage> cacheParameters();

  public abstract UserParameters userParameters();

  @JsonIgnore
  public abstract TemporalNetworkFactory<K> networkFactory();

  @JsonIgnore
  public abstract RiskScoreFactory<K> scoreFactory();

  public abstract Set<Class<? extends LogRecord>> loggable();

  public abstract Context context();

  @Value.Lazy
  public TemporalNetwork<K> contactNetwork() {
    return networkFactory().getNetwork();
  }

  @Value.Derived
  public String id() {
    return Identifiers.newIntString();
  }

  @JsonIgnore
  public ExperimentState<K> withNewNetwork() {
    return ExperimentState.copyOf(this);
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
    TemporalNetworkLogger.logMetrics(contactNetwork());
    LOGGER.log(SettingsRecord.KEY, ExperimentSettings.class, this::settings);
  }

  private void runAlgorithm() {
    RiskPropagationBuilder.<K>create()
        .userParameters(userParameters())
        .scoreFactory(scoreFactory())
        .contactNetwork(contactNetwork())
        .cacheFactory(() -> IntervalCache.create(cacheParameters()))
        .clock(context().clock())
        .build()
        .run();
  }

  private ExperimentSettings settings() {
    return ExperimentSettings.builder()
        .stateId(id())
        .context(context())
        .userParameters(userParameters())
        .cacheParameters(cacheParameters())
        .networkType(contactNetwork().type())
        .networkId(contactNetwork().id())
        .build();
  }
}

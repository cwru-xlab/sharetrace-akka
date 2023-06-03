package io.sharetrace.experiment;

import io.sharetrace.actor.RiskPropagationBuilder;
import io.sharetrace.experiment.data.RiskScoreFactory;
import io.sharetrace.graph.NetworkLogger;
import io.sharetrace.graph.TemporalNetwork;
import io.sharetrace.graph.TemporalNetworkFactory;
import io.sharetrace.model.Identifiable;
import io.sharetrace.model.UserParameters;
import io.sharetrace.model.message.RiskScoreMessage;
import io.sharetrace.util.Identifiers;
import io.sharetrace.util.cache.CacheParameters;
import io.sharetrace.util.cache.IntervalCache;
import io.sharetrace.util.logging.LogRecord;
import io.sharetrace.util.logging.Logging;
import io.sharetrace.util.logging.RecordLogger;
import io.sharetrace.util.logging.setting.ExperimentSettings;
import io.sharetrace.util.logging.setting.SettingsRecord;
import java.util.Set;
import java.util.stream.IntStream;
import org.immutables.value.Value;

@Value.Immutable
abstract class BaseExperimentState<T> implements Runnable, Identifiable {

  private static final RecordLogger<SettingsRecord> LOGGER = Logging.settingsLogger();

  public abstract CacheParameters<RiskScoreMessage> cacheParameters();

  public abstract UserParameters userParameters();

  public abstract TemporalNetworkFactory<T> networkFactory();

  public abstract RiskScoreFactory<T> scoreFactory();

  public abstract Set<Class<? extends LogRecord>> loggable();

  public abstract Context context();

  @Value.Lazy
  public TemporalNetwork<T> contactNetwork() {
    return networkFactory().getNetwork();
  }

  @Value.Derived
  public String id() {
    return Identifiers.newIntString();
  }

  public ExperimentState<T> withNewNetwork() {
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
    NetworkLogger.logMetrics(contactNetwork());
    LOGGER.log(SettingsRecord.KEY, ExperimentSettings.class, this::settings);
  }

  private void runAlgorithm() {
    RiskPropagationBuilder.<T>create()
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

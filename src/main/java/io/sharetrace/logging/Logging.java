package io.sharetrace.logging;

import ch.qos.logback.core.spi.PropertyContainer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.LoggerFactory;

public final class Logging {

  public static final String EVENTS_LOGGER_NAME = "EventsLogger";
  public static final String METRICS_LOGGER_NAME = "MetricsLogger";
  public static final String SETTINGS_LOGGER_NAME = "SettingsLogger";

  private static final String GRAPHS_PATH = "graphs.log.dir";
  private static final String STATE_ID = "sid";

  private Logging() {}

  public static Logger metricsLogger(Set<Class<? extends Loggable>> loggable) {
    return logger(loggable, () -> LoggerFactory.getLogger(METRICS_LOGGER_NAME));
  }

  public static Logger logger(
      Set<Class<? extends Loggable>> loggable, Supplier<org.slf4j.Logger> logger) {
    return DefaultLogger.of(loggable, logger);
  }

  public static Logger settingsLogger(Set<Class<? extends Loggable>> loggable) {
    return logger(loggable, () -> LoggerFactory.getLogger(SETTINGS_LOGGER_NAME));
  }

  public static Path graphsPath() {
    PropertyContainer properties = (PropertyContainer) LoggerFactory.getILoggerFactory();
    return Path.of(properties.getProperty(GRAPHS_PATH));
  }

  public static Map<String, String> mdc(String stateId) {
    return Map.of(STATE_ID, stateId);
  }
}

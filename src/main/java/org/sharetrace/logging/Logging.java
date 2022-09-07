package org.sharetrace.logging;

import ch.qos.logback.core.spi.PropertyContainer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.sharetrace.experiment.GraphType;
import org.slf4j.LoggerFactory;

public final class Logging {

  private static final String SETTINGS_LOGGER_NAME = "SettingsLogger";
  private static final String EVENTS_LOGGER_NAME = "EventsLogger";
  private static final String METRICS_LOGGER_NAME = "MetricsLogger";

  private Logging() {}

  public static Logger logger(
      Set<Class<? extends Loggable>> loggable, Supplier<org.slf4j.Logger> logger) {
    return DefaultLogger.of(loggable, logger);
  }

  public static String eventsLoggerName() {
    return EVENTS_LOGGER_NAME;
  }

  public static Logger metricsLogger(Set<Class<? extends Loggable>> loggable) {
    return DefaultLogger.of(loggable, LoggerFactory.getLogger(metricsLoggerName()));
  }

  public static String metricsLoggerName() {
    return METRICS_LOGGER_NAME;
  }

  public static Logger settingsLogger(Set<Class<? extends Loggable>> loggable) {
    return DefaultLogger.of(loggable, LoggerFactory.getLogger(settingsLoggerName()));
  }

  public static String settingsLoggerName() {
    return SETTINGS_LOGGER_NAME;
  }

  public static Path graphsPath() {
    PropertyContainer properties = (PropertyContainer) LoggerFactory.getILoggerFactory();
    return Path.of(properties.getProperty("graphs.log.dir"));
  }

  public static Map<String, String> mdc(String stateId, GraphType graphType) {
    return Map.of("stateId", stateId, "graphType", graphType.toString());
  }
}

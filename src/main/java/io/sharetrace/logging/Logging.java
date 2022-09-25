package io.sharetrace.logging;

import ch.qos.logback.core.spi.PropertyContainer;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.LoggerFactory;

public final class Logging {

  private Logging() {}

  public static Logger logger(
      Set<Class<? extends Loggable>> loggable, Supplier<org.slf4j.Logger> logger) {
    return DefaultLogger.of(loggable, logger);
  }

  public static String eventsLoggerName() {
    return "EventsLogger";
  }

  public static Logger metricsLogger(Set<Class<? extends Loggable>> loggable) {
    return logger(loggable, () -> LoggerFactory.getLogger(metricsLoggerName()));
  }

  public static String metricsLoggerName() {
    return "MetricsLogger";
  }

  public static Logger settingsLogger(Set<Class<? extends Loggable>> loggable) {
    return logger(loggable, () -> LoggerFactory.getLogger("SettingsLogger"));
  }

  public static Path graphsPath() {
    PropertyContainer properties = (PropertyContainer) LoggerFactory.getILoggerFactory();
    return Path.of(properties.getProperty("graphs.log.dir"));
  }

  public static Map<String, String> mdc(String stateId) {
    return Map.of("sid", stateId);
  }
}

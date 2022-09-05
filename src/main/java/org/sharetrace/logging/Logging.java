package org.sharetrace.logging;

import ch.qos.logback.core.spi.PropertyContainer;
import java.nio.file.Path;
import java.util.Map;
import org.sharetrace.experiment.GraphType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Logging {

  public static final String METRIC_LOGGER_NAME = "MetricLogger";
  public static final String EVENT_LOGGER_NAME = "EventLogger";
  public static final String SETTING_LOGGER_NAME = "SettingLogger";

  private Logging() {}

  public static Logger metricLogger() {
    return LoggerFactory.getLogger(METRIC_LOGGER_NAME);
  }

  public static Logger settingLogger() {
    return LoggerFactory.getLogger(SETTING_LOGGER_NAME);
  }

  public static Path graphsLogPath() {
    PropertyContainer properties = (PropertyContainer) LoggerFactory.getILoggerFactory();
    return Path.of(properties.getProperty("graphs.log.dir"));
  }

  public static Map<String, String> mdc(String stateId, GraphType graphType) {
    return Map.of("stateId", stateId, "graphType", graphType.toString());
  }
}

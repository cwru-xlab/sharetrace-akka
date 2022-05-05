package org.sharetrace.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Loggers {

  private static final String METRIC_LOGGER_NAME = "MetricLogger";
  private static final String EVENT_LOGGER_NAME = "EventLogger";

  private Loggers() {}

  public static Logger eventLogger() {
    return LoggerFactory.getLogger(EVENT_LOGGER_NAME);
  }

  public static String eventLoggerName() {
    return EVENT_LOGGER_NAME;
  }

  public static Logger metricLogger() {
    return LoggerFactory.getLogger(METRIC_LOGGER_NAME);
  }

  public static String metricLoggerName() {
    return METRIC_LOGGER_NAME;
  }
}

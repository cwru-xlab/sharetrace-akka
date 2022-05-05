package org.sharetrace.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Loggers {

  private static final String METRICS = "MetricLogger";
  private static final String EVENTS = "EventLogger";

  private Loggers() {}

  public static Logger eventsLogger() {
    return LoggerFactory.getLogger(EVENTS);
  }

  public static String eventsLoggerName() {
    return EVENTS;
  }

  public static Logger metricsLogger() {
    return LoggerFactory.getLogger(METRICS);
  }

  public static String metricsLoggerName() {
    return METRICS;
  }
}

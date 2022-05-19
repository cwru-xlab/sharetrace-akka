package org.sharetrace.logging;

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
}
